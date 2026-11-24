package com.airline.controller;

import com.airline.domain.BookingStatus;
import com.airline.domain.ImportMode;
import com.airline.domain.PriorityLevel;
import com.airline.dto.BookingDtos.BookingRequest;
import com.airline.dto.BookingDtos.BookingResponse;
import com.airline.dto.BookingDtos.BookingUpdateRequest;
import com.airline.dto.BookingDtos.BulkRequest;
import com.airline.dto.BookingDtos.BulkResponse;
import com.airline.dto.NoteDtos.NoteRequest;
import com.airline.dto.NoteDtos.NoteResponse;
import com.airline.dto.PageResponse;
import com.airline.error.BadRequestException;
import com.airline.filter.BookingFilter;
import com.airline.filter.PriceComparison;
import com.airline.service.BookingService;
import com.airline.service.CsvService;
import com.airline.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookings;
    private final NoteService notes;
    private final CsvService csv;

    public BookingController(BookingService bookings, NoteService notes, CsvService csv) {
        this.bookings = bookings;
        this.notes = notes;
        this.csv = csv;
    }

    @GetMapping
    public PageResponse<BookingResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID passengerId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Boolean refundable,
            @RequestParam(required = false) PriorityLevel priority,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "active") String view,
            @RequestParam(required = false) String priceOp,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "departureDate") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        return bookings.list(filter(q, passengerId, status, refundable, priority, tag, view, priceOp, price),
                page, size, sort, direction);
    }

    @GetMapping("/{id}")
    public BookingResponse get(@PathVariable UUID id) {
        return bookings.get(id);
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(201).body(bookings.create(request));
    }

    @PutMapping("/{id}")
    public BookingResponse update(@PathVariable UUID id, @Valid @RequestBody BookingUpdateRequest request) {
        return bookings.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable UUID id) {
        bookings.archive(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<Void> restore(@PathVariable UUID id) {
        bookings.restore(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> permanent(@PathVariable UUID id) {
        bookings.permanentDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk")
    public BulkResponse bulk(@Valid @RequestBody BulkRequest request) {
        return bookings.bulk(request);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID passengerId,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) Boolean refundable,
            @RequestParam(required = false) PriorityLevel priority,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "active") String view,
            @RequestParam(required = false) String priceOp,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(defaultValue = "departureDate") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        byte[] body = csv.export(filter(q, passengerId, status, refundable, priority, tag, view, priceOp, price), sort, direction);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("bookings.csv", StandardCharsets.UTF_8).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(body);
    }

    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public com.airline.dto.AdminDtos.ImportPreviewResponse preview(@RequestParam("file") MultipartFile file,
                                                                  @RequestParam ImportMode mode) {
        return csv.preview(file, mode);
    }

    @PostMapping(value = "/import/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public com.airline.dto.AdminDtos.ImportApplyResponse apply(@RequestParam("file") MultipartFile file,
                                                               @RequestParam ImportMode mode) {
        return csv.apply(file, mode);
    }

    @GetMapping("/{id}/notes")
    public List<NoteResponse> notes(@PathVariable UUID id) {
        return notes.list(id);
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<NoteResponse> addNote(@PathVariable UUID id, @Valid @RequestBody NoteRequest request) {
        return ResponseEntity.status(201).body(notes.create(id, request));
    }

    @PutMapping("/{id}/notes/{noteId}")
    public NoteResponse updateNote(@PathVariable UUID id, @PathVariable UUID noteId, @Valid @RequestBody NoteRequest request) {
        return notes.update(id, noteId, request);
    }

    @DeleteMapping("/{id}/notes/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable UUID id, @PathVariable UUID noteId) {
        notes.delete(id, noteId);
        return ResponseEntity.noContent().build();
    }

    private BookingFilter filter(String q, UUID passengerId, BookingStatus status, Boolean refundable,
                                 PriorityLevel priority, String tag, String view, String priceOp, BigDecimal price) {
        if (!view.equals("active") && !view.equals("archived") && !view.equals("all")) {
            throw new BadRequestException("Фильтр архива: active, archived или all.");
        }
        boolean hasPrice = price != null;
        boolean hasOp = priceOp != null && !priceOp.isBlank();
        if (hasPrice != hasOp) {
            throw new BadRequestException("Для фильтра по цене укажите и значение, и операцию.");
        }
        return new BookingFilter(q, passengerId, status, refundable, priority, tag, view,
                hasOp ? PriceComparison.parse(priceOp) : null, price);
    }
}
