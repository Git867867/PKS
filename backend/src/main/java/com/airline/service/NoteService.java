package com.airline.service;

import com.airline.domain.Booking;
import com.airline.domain.BookingNote;
import com.airline.domain.UserAccount;
import com.airline.dto.NoteDtos.NoteRequest;
import com.airline.dto.NoteDtos.NoteResponse;
import com.airline.error.NotFoundException;
import com.airline.repository.BookingNoteRepository;
import com.airline.repository.BookingRepository;
import com.airline.repository.UserAccountRepository;
import com.airline.security.Auth;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NoteService {

    private final BookingRepository bookings;
    private final BookingNoteRepository notes;
    private final UserAccountRepository users;
    private final AuditService audit;

    public NoteService(BookingRepository bookings, BookingNoteRepository notes, UserAccountRepository users, AuditService audit) {
        this.bookings = bookings;
        this.notes = notes;
        this.users = users;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> list(UUID bookingId) {
        Booking booking = requireBooking(bookingId);
        return notes.findByBookingIdOrderByPinnedDescCreatedAtDesc(booking.getId()).stream()
                .map(NoteResponse::from)
                .toList();
    }

    @Transactional
    public NoteResponse create(UUID bookingId, NoteRequest request) {
        Booking booking = requireBooking(bookingId);
        UserAccount author = users.findById(Auth.require().getId())
                .orElseThrow(() -> new NotFoundException("Пользователь не найден."));
        BookingNote note = new BookingNote();
        note.setBooking(booking);
        note.setAuthor(author);
        note.setText(request.text().trim());
        note.setPinned(request.pinned());
        notes.save(note);
        audit.record(author.getId(), "NOTE_CREATED", "BookingNote", note.getId(),
                "Заметка к " + booking.getBookingCode());
        return NoteResponse.from(note);
    }

    @Transactional
    public NoteResponse update(UUID bookingId, UUID noteId, NoteRequest request) {
        Booking booking = requireBooking(bookingId);
        BookingNote note = notes.findByIdAndBookingId(noteId, booking.getId())
                .orElseThrow(() -> new NotFoundException("Заметка не найдена."));
        note.setText(request.text().trim());
        note.setPinned(request.pinned());
        audit.record(Auth.require().getId(), "NOTE_UPDATED", "BookingNote", note.getId(),
                "Изменена заметка к " + booking.getBookingCode());
        return NoteResponse.from(note);
    }

    @Transactional
    public void delete(UUID bookingId, UUID noteId) {
        Booking booking = requireBooking(bookingId);
        BookingNote note = notes.findByIdAndBookingId(noteId, booking.getId())
                .orElseThrow(() -> new NotFoundException("Заметка не найдена."));
        notes.delete(note);
        audit.record(Auth.require().getId(), "NOTE_DELETED", "BookingNote", noteId,
                "Удалена заметка к " + booking.getBookingCode());
    }

    private Booking requireBooking(UUID id) {
        return bookings.findByPublicId(id).orElseThrow(() -> new NotFoundException("Бронирование не найдено."));
    }
}
