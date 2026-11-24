package com.airline.repository;

import com.airline.domain.BookingNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingNoteRepository extends JpaRepository<BookingNote, UUID> {

    List<BookingNote> findByBookingIdOrderByPinnedDescCreatedAtDesc(Long bookingId);

    Optional<BookingNote> findByIdAndBookingId(UUID id, Long bookingId);
}
