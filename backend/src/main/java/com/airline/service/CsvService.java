package com.airline.service;

import com.airline.domain.Booking;
import com.airline.domain.BookingStatus;
import com.airline.domain.ImportMode;
import com.airline.domain.Passenger;
import com.airline.dto.AdminDtos.ImportApplyResponse;
import com.airline.dto.AdminDtos.ImportPreviewResponse;
import com.airline.dto.AdminDtos.ImportRowError;
import com.airline.error.BadRequestException;
import com.airline.filter.BookingFilter;
import com.airline.repository.BookingRepository;
import com.airline.repository.PassengerRepository;
import com.airline.security.Auth;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CsvService {

    static final String[] HEADERS = {
            "bookingCode", "flightNumber", "passengerEmail", "departureCity", "arrivalCity",
            "departureDate", "price", "status", "refundable", "tags"
    };
    private static final DateTimeFormatter MINUTES = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_ROWS = 500;

    private final BookingRepository bookings;
    private final PassengerRepository passengers;
    private final BookingService bookingService;
    private final AuditService audit;

    public CsvService(BookingRepository bookings, PassengerRepository passengers,
                      BookingService bookingService, AuditService audit) {
        this.bookings = bookings;
        this.passengers = passengers;
        this.bookingService = bookingService;
        this.audit = audit;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public byte[] export(BookingFilter filter, String sort, String direction) {
        List<Booking> rows = bookingService.findForExport(filter, sort, direction);
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bytes.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            CSVFormat format = CSVFormat.DEFAULT.builder().setDelimiter(';').setHeader(HEADERS).build();
            try (CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(bytes, StandardCharsets.UTF_8), format)) {
                for (Booking booking : rows) {
                    printer.printRecord(
                            booking.getBookingCode(),
                            booking.getFlightNumber(),
                            booking.getPassenger().getEmail(),
                            booking.getDepartureCity(),
                            booking.getArrivalCity(),
                            booking.getDepartureDate().format(MINUTES),
                            booking.getPrice().toPlainString(),
                            booking.getStatus().name(),
                            Boolean.toString(booking.isRefundable()),
                            String.join(",", booking.getTags().stream().map(tag -> tag.getName()).sorted().toList())
                    );
                }
            }
            return bytes.toByteArray();
        } catch (IOException ex) {
            throw new BadRequestException("Не удалось сформировать CSV.");
        }
    }

    public ImportPreviewResponse preview(MultipartFile file, ImportMode mode) {
        List<ParsedRow> rows = read(file);
        List<ImportRowError> errors = new ArrayList<>();
        int valid = 0;
        Set<String> seen = new HashSet<>();
        for (ParsedRow row : rows) {
            String error = row.error != null ? row.error : validate(row, mode, seen);
            if (error == null) {
                valid++;
            } else {
                errors.add(new ImportRowError(row.number, error));
            }
        }
        return new ImportPreviewResponse(valid, errors.size(), errors);
    }

    public ImportApplyResponse apply(MultipartFile file, ImportMode mode) {
        List<ParsedRow> rows = read(file);
        List<ImportRowError> errors = new ArrayList<>();
        int created = 0;
        int updated = 0;
        Set<String> seen = new HashSet<>();
        for (ParsedRow row : rows) {
            String error = row.error != null ? row.error : validate(row, mode, seen);
            if (error != null) {
                errors.add(new ImportRowError(row.number, error));
                continue;
            }
            try {
                boolean isNew = row.code.isBlank() || bookings.findByBookingCode(row.code).isEmpty();
                write(row);
                if (isNew) {
                    created++;
                } else {
                    updated++;
                }
            } catch (RuntimeException ex) {
                String message = ex instanceof com.airline.error.ApiException api
                        ? api.getMessage() : "Не удалось сохранить строку.";
                errors.add(new ImportRowError(row.number, message));
            }
        }
        audit.record(Auth.require().getId(), "IMPORT_APPLIED", "Booking", null,
                "Импорт CSV: создано " + created + ", обновлено " + updated);
        return new ImportApplyResponse(created, updated, errors.size(), errors);
    }

    private void write(ParsedRow row) {
        if (row.code.isBlank() || bookings.findByBookingCode(row.code).isEmpty()) {
            bookingService.importCreate(row.code, row.email, row.flight, row.from, row.to, row.departure,
                    row.price, row.refundable, row.tags);
        } else {
            bookingService.importUpdate(row.code, row.email, row.flight, row.from, row.to, row.departure,
                    row.price, row.status == null ? BookingStatus.CREATED : row.status, row.refundable, row.tags);
        }
    }

    private String validate(ParsedRow row, ImportMode mode, Set<String> seen) {
        try {
            if (!row.code.isBlank() && !seen.add(row.code)) {
                throw new BadRequestException("Код повторяется в файле.");
            }
            Passenger passenger = passengers.findByEmailIgnoreCase(row.email)
                    .orElseThrow(() -> new BadRequestException("Пассажир с таким email не найден."));
            Booking existing = row.code.isBlank() ? null : bookings.findByBookingCode(row.code).orElse(null);
            if (existing == null) {
                if (row.status != null && row.status != BookingStatus.CREATED) {
                    throw new BadRequestException("Новое бронирование создается только со статусом CREATED.");
                }
                if (!passenger.isActive()) {
                    throw new BadRequestException("Пассажир отключен.");
                }
                checkFields(row, null);
            } else {
                if (mode == ImportMode.CREATE_ONLY) {
                    throw new BadRequestException("Код уже есть. В этом режиме обновляются только новые записи.");
                }
                if (existing.isArchived()) {
                    throw new BadRequestException("Бронирование в архиве.");
                }
                BookingRules.checkStatusChange(existing.getStatus(), row.status);
                checkFields(row, existing.getDepartureDate());
            }
            if (row.tags != null) {
                for (String tag : row.tags) {
                    if (tag.length() > 40 || !tag.matches("[a-zа-яё0-9\\- ]+")) {
                        throw new BadRequestException("Некорректный тег «" + tag + "».");
                    }
                }
            }
            return null;
        } catch (BadRequestException ex) {
            return ex.getMessage();
        }
    }

    private void checkFields(ParsedRow row, LocalDateTime previousDeparture) {
        BookingRules.checkFlight(row.flight);
        BookingRules.checkCities(row.from, row.to);
        BookingRules.checkPrice(row.price);
        BookingRules.checkDeparture(row.departure, previousDeparture);
    }

    private List<ParsedRow> read(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Файл пуст.");
        }
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(';')
                .setHeader(HEADERS)
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();
        List<ParsedRow> rows = new ArrayList<>();
        try (Reader reader = stripBom(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = format.parse(reader)) {
            for (CSVRecord record : parser) {
                if (rows.size() >= MAX_ROWS) {
                    throw new BadRequestException("В файле больше " + MAX_ROWS + " строк.");
                }
                rows.add(parse(record));
            }
        } catch (IOException ex) {
            throw new BadRequestException("Не удалось прочитать CSV. Проверьте кодировку UTF-8 и разделитель «;».");
        }
        if (rows.isEmpty()) {
            throw new BadRequestException("В файле нет строк с данными.");
        }
        return rows;
    }

    private ParsedRow parse(CSVRecord record) {
        int number = (int) record.getRecordNumber();
        try {
            String code = text(record, "bookingCode").toUpperCase(Locale.ROOT);
            String statusText = text(record, "status");
            BookingStatus status = statusText.isBlank() ? null : BookingStatus.valueOf(statusText.toUpperCase(Locale.ROOT));
            List<String> tags = new ArrayList<>();
            String tagText = text(record, "tags");
            if (!tagText.isBlank()) {
                for (String tag : tagText.split(",")) {
                    if (!tag.isBlank()) {
                        tags.add(tag.trim().toLowerCase(Locale.ROOT));
                    }
                }
            }
            return new ParsedRow(number, code, text(record, "flightNumber").toUpperCase(Locale.ROOT),
                    text(record, "passengerEmail"), text(record, "departureCity"), text(record, "arrivalCity"),
                    parseDate(text(record, "departureDate")), parsePrice(text(record, "price")),
                    status, parseFlag(text(record, "refundable")), tags, null);
        } catch (RuntimeException ex) {
            return new ParsedRow(number, "", "", "", "", "", null, null, null, false, List.of(),
                    "Не удалось разобрать строку. Проверьте столбцы, дату и статус.");
        }
    }

    private String text(CSVRecord record, String name) {
        return record.get(name) == null ? "" : record.get(name).trim();
    }

    private LocalDateTime parseDate(String value) {
        try {
            return LocalDateTime.parse(value, MINUTES);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(value, SECONDS);
            } catch (DateTimeParseException ex) {
                return LocalDateTime.parse(value);
            }
        }
    }

    private BigDecimal parsePrice(String value) {
        return new BigDecimal(value.replace(',', '.'));
    }

    private boolean parseFlag(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.equals("true") || normalized.equals("да") || normalized.equals("1")) {
            return true;
        }
        if (normalized.equals("false") || normalized.equals("нет") || normalized.equals("0") || normalized.isBlank()) {
            return false;
        }
        throw new IllegalArgumentException("refundable");
    }

    private Reader stripBom(Reader reader) throws IOException {
        java.io.BufferedReader buffered = new java.io.BufferedReader(reader);
        buffered.mark(1);
        if (buffered.read() != '\uFEFF') {
            buffered.reset();
        }
        return buffered;
    }

    private record ParsedRow(
            int number,
            String code,
            String flight,
            String email,
            String from,
            String to,
            LocalDateTime departure,
            BigDecimal price,
            BookingStatus status,
            boolean refundable,
            List<String> tags,
            String error
    ) {
    }
}
