package com.airline.service;

import com.airline.config.AppProperties;
import com.airline.domain.Booking;
import com.airline.domain.BookingStatus;
import com.airline.domain.Passenger;
import com.airline.domain.Tag;
import com.airline.dto.BookingDtos.BookingRequest;
import com.airline.dto.BookingDtos.BookingResponse;
import com.airline.dto.BookingDtos.BookingUpdateRequest;
import com.airline.dto.BookingDtos.BulkFailure;
import com.airline.dto.BookingDtos.BulkRequest;
import com.airline.dto.BookingDtos.BulkResponse;
import com.airline.dto.PageResponse;
import com.airline.error.BadRequestException;
import com.airline.error.ConflictException;
import com.airline.error.ForbiddenException;
import com.airline.error.NotFoundException;
import com.airline.filter.BookingFilter;
import com.airline.filter.BookingSpecifications;
import com.airline.filter.PageRequests;
import com.airline.repository.BookingRepository;
import com.airline.repository.PassengerRepository;
import com.airline.repository.TagRepository;
import com.airline.security.Auth;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class BookingService {

    private static final Set<String> SORT_FIELDS = Set.of("departureDate", "price", "status", "createdAt");

    private final BookingRepository bookings;
    private final PassengerRepository passengers;
    private final TagRepository tags;
    private final AuditService audit;
    private final AppProperties properties;
    private final TransactionTemplate transactions;

    public BookingService(BookingRepository bookings, PassengerRepository passengers, TagRepository tags,
                          AuditService audit, AppProperties properties, PlatformTransactionManager manager) {
        this.bookings = bookings;
        this.passengers = passengers;
        this.tags = tags;
        this.audit = audit;
        this.properties = properties;
        this.transactions = new TransactionTemplate(manager);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> list(BookingFilter filter, int page, int size, String sort, String direction) {
        Pageable pageable = PageRequests.of(page, size, sort, direction, properties.getSecurity().getMaxPageSize(), SORT_FIELDS);
        Page<BookingResponse> result = bookings.findAll(BookingSpecifications.byFilter(filter), pageable)
                .map(BookingResponse::from);
        return PageResponse.of(result);
    }

    @Transactional(readOnly = true)
    public List<Booking> findForExport(BookingFilter filter, String sort, String direction) {
        Pageable probe = PageRequests.of(0, 1, sort, direction, properties.getSecurity().getMaxPageSize(), SORT_FIELDS);
        Sort sorting = probe.getSort();
        return bookings.findAll(BookingSpecifications.byFilter(filter), sorting);
    }

    @Transactional(readOnly = true)
    public BookingResponse get(UUID id) {
        return BookingResponse.from(require(id));
    }

    @Transactional
    public BookingResponse create(BookingRequest request) {
        Booking booking = new Booking();
        booking.setBookingCode(nextCode());
        booking.setStatus(BookingStatus.CREATED);
        booking.setArchived(false);
        fill(booking, request.passengerId(), request.flightNumber(), request.departureCity(), request.arrivalCity(),
                request.departureDate(), request.price(), request.refundable(), request.tags(), null);
        bookings.save(booking);
        audit.record(Auth.require().getId(), "BOOKING_CREATED", "Booking", booking.getPublicId(),
                "Создано бронирование " + booking.getBookingCode());
        return BookingResponse.from(booking);
    }

    @Transactional
    public BookingResponse update(UUID id, BookingUpdateRequest request) {
        Booking booking = require(id);
        if (booking.isArchived()) {
            throw new BadRequestException("Сначала восстановите бронирование из архива.");
        }
        checkVersion(booking, request.version());
        BookingRules.checkStatusChange(booking.getStatus(), request.status());
        booking.setStatus(request.status());
        fill(booking, request.passengerId(), request.flightNumber(), request.departureCity(), request.arrivalCity(),
                request.departureDate(), request.price(), request.refundable(), request.tags(), booking.getDepartureDate());
        audit.record(Auth.require().getId(), "BOOKING_UPDATED", "Booking", booking.getPublicId(),
                "Изменено бронирование " + booking.getBookingCode());
        return BookingResponse.from(booking);
    }

    @Transactional
    public void archive(UUID id) {
        Booking booking = require(id);
        booking.setArchived(true);
        audit.record(Auth.require().getId(), "BOOKING_ARCHIVED", "Booking", booking.getPublicId(),
                "В архиве " + booking.getBookingCode());
    }

    @Transactional
    public void restore(UUID id) {
        Booking booking = require(id);
        booking.setArchived(false);
        audit.record(Auth.require().getId(), "BOOKING_RESTORED", "Booking", booking.getPublicId(),
                "Восстановлено " + booking.getBookingCode());
    }

    @Transactional
    public void permanentDelete(UUID id) {
        if (!properties.getSecurity().isPermanentDeleteEnabled()) {
            throw new ForbiddenException("Окончательное удаление выключено.");
        }
        Booking booking = require(id);
        if (!booking.isArchived()) {
            throw new BadRequestException("Сначала отправьте бронирование в архив.");
        }
        audit.record(Auth.require().getId(), "BOOKING_DELETED", "Booking", booking.getPublicId(),
                "Удалено " + booking.getBookingCode());
        bookings.delete(booking);
    }

    public BulkResponse bulk(BulkRequest request) {
        if (request.action() == com.airline.domain.BulkAction.SET_STATUS && request.status() == null) {
            throw new BadRequestException("Для смены статуса укажите новый статус.");
        }
        if ((request.action() == com.airline.domain.BulkAction.ADD_TAG
                || request.action() == com.airline.domain.BulkAction.REMOVE_TAG)
                && (request.tag() == null || request.tag().isBlank())) {
            throw new BadRequestException("Укажите тег.");
        }
        List<UUID> succeeded = new ArrayList<>();
        List<BulkFailure> failed = new ArrayList<>();
        for (UUID id : request.ids()) {
            try {
                transactions.executeWithoutResult(status -> applyBulk(id, request));
                succeeded.add(id);
            } catch (RuntimeException ex) {
                String message = ex instanceof com.airline.error.ApiException api ? api.getMessage() : "Не удалось обработать запись.";
                failed.add(new BulkFailure(id, message));
            }
        }
        audit.record(Auth.require().getId(), "BOOKING_BULK", "Booking", null,
                "Массовая операция " + request.action() + ": успешно " + succeeded.size() + ", ошибок " + failed.size());
        return new BulkResponse(succeeded, failed);
    }

    private void applyBulk(UUID id, BulkRequest request) {
        Booking booking = require(id);
        switch (request.action()) {
            case ARCHIVE -> booking.setArchived(true);
            case RESTORE -> booking.setArchived(false);
            case SET_STATUS -> {
                if (booking.isArchived()) {
                    throw new BadRequestException("Запись в архиве.");
                }
                BookingRules.checkStatusChange(booking.getStatus(), request.status());
                booking.setStatus(request.status());
                booking.setPriority(PriorityCalculator.calculate(booking.getStatus(), booking.getPrice(),
                        booking.getDepartureDate(), LocalDateTime.now()));
            }
            case ADD_TAG -> booking.getTags().add(findOrCreateTag(request.tag()));
            case REMOVE_TAG -> booking.getTags().removeIf(tag -> tag.getName().equalsIgnoreCase(request.tag().trim()));
        }
    }

    @Transactional
    public Booking importCreate(String code, String passengerEmail, String flight, String from, String to,
                                java.time.LocalDateTime departure, java.math.BigDecimal price, boolean refundable,
                                List<String> tagNames) {
        Passenger passenger = passengers.findByEmailIgnoreCase(passengerEmail)
                .orElseThrow(() -> new NotFoundException("Пассажир не найден."));
        Booking booking = new Booking();
        booking.setBookingCode(code == null || code.isBlank() ? nextCode() : code.trim().toUpperCase());
        if (bookings.existsByBookingCode(booking.getBookingCode())) {
            throw new BadRequestException("Код " + booking.getBookingCode() + " уже есть.");
        }
        booking.setPassenger(requireActive(passenger));
        booking.setStatus(BookingStatus.CREATED);
        booking.setArchived(false);
        applyValues(booking, flight, from, to, departure, price, refundable, tagNames, null);
        return bookings.save(booking);
    }

    @Transactional
    public Booking importUpdate(String code, String passengerEmail, String flight, String from, String to,
                                java.time.LocalDateTime departure, java.math.BigDecimal price, BookingStatus status,
                                boolean refundable, List<String> tagNames) {
        Booking booking = bookings.findByBookingCode(code)
                .orElseThrow(() -> new NotFoundException("Бронирование не найдено."));
        Passenger passenger = passengers.findByEmailIgnoreCase(passengerEmail)
                .orElseThrow(() -> new NotFoundException("Пассажир не найден."));
        if (booking.isArchived()) {
            throw new BadRequestException("Бронирование в архиве. Сначала восстановите его.");
        }
        if (!passenger.isActive() && !passenger.getId().equals(booking.getPassenger().getId())) {
            throw new BadRequestException("Пассажир отключен.");
        }
        booking.setPassenger(passenger);
        BookingRules.checkStatusChange(booking.getStatus(), status);
        booking.setStatus(status);
        applyValues(booking, flight, from, to, departure, price, refundable, tagNames, booking.getDepartureDate());
        return booking;
    }

    private void fill(Booking booking, UUID passengerId, String flight, String from, String to,
                      java.time.LocalDateTime departure, java.math.BigDecimal price, boolean refundable,
                      List<String> tagNames, java.time.LocalDateTime previousDeparture) {
        Passenger passenger = passengers.findByPublicId(passengerId)
                .orElseThrow(() -> new NotFoundException("Пассажир не найден."));
        if (!passenger.isActive() && (booking.getPassenger() == null || !passenger.getId().equals(booking.getPassenger().getId()))) {
            throw new BadRequestException("Пассажир отключен. Новое бронирование на него создать нельзя.");
        }
        booking.setPassenger(passenger);
        applyValues(booking, flight, from, to, departure, price, refundable, tagNames, previousDeparture);
    }

    private void applyValues(Booking booking, String flight, String from, String to, java.time.LocalDateTime departure,
                             java.math.BigDecimal price, boolean refundable, List<String> tagNames,
                             java.time.LocalDateTime previousDeparture) {
        String normalizedFlight = flight == null ? null : flight.trim().toUpperCase();
        BookingRules.checkFlight(normalizedFlight);
        BookingRules.checkCities(from == null ? null : from.trim(), to == null ? null : to.trim());
        BookingRules.checkPrice(price);
        BookingRules.checkDeparture(departure, previousDeparture);
        booking.setFlightNumber(normalizedFlight);
        booking.setDepartureCity(from.trim());
        booking.setArrivalCity(to.trim());
        booking.setDepartureDate(departure);
        booking.setPrice(price);
        booking.setRefundable(refundable);
        booking.setPriority(PriorityCalculator.calculate(booking.getStatus(), price, departure, LocalDateTime.now()));
        replaceTags(booking, tagNames);
    }

    private void replaceTags(Booking booking, List<String> names) {
        booking.getTags().clear();
        if (names == null) {
            return;
        }
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            booking.getTags().add(findOrCreateTag(name));
        }
    }

    private Tag findOrCreateTag(String raw) {
        String name = raw.trim().toLowerCase();
        if (name.length() > 40 || !name.matches("[a-zа-яё0-9\\- ]+")) {
            throw new BadRequestException("Тег «" + raw.trim() + "» содержит недопустимые символы или слишком длинный.");
        }
        return tags.findByName(name).orElseGet(() -> {
            Tag tag = new Tag();
            tag.setName(name);
            return tags.save(tag);
        });
    }

    private Passenger requireActive(Passenger passenger) {
        if (!passenger.isActive()) {
            throw new BadRequestException("Пассажир отключен.");
        }
        return passenger;
    }

    private void checkVersion(Booking booking, Long version) {
        if (version == null || !version.equals(booking.getVersion())) {
            throw new ConflictException("Запись уже изменили. Обновите страницу и повторите.");
        }
    }

    private Booking require(UUID id) {
        return bookings.findByPublicId(id).orElseThrow(() -> new NotFoundException("Бронирование не найдено."));
    }

    private String nextCode() {
        long number = bookings.count() + 1;
        String code = String.format("BK-%06d", number);
        while (bookings.existsByBookingCode(code)) {
            number++;
            code = String.format("BK-%06d", number);
        }
        return code;
    }
}
