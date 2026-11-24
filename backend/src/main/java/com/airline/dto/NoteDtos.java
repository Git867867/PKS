package com.airline.dto;

import com.airline.domain.BookingNote;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public final class NoteDtos {
    private NoteDtos() {
    }

    public record NoteRequest(
            @NotBlank(message = "Введите текст заметки.") @Size(max = 1000) String text,
            boolean pinned
    ) {
    }

    public record NoteResponse(
            UUID id,
            String text,
            boolean pinned,
            String authorName,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static NoteResponse from(BookingNote note) {
            String author = note.getAuthor() == null ? "—" : note.getAuthor().getFullName();
            return new NoteResponse(note.getId(), note.getText(), note.isPinned(),
                    author, note.getCreatedAt(), note.getUpdatedAt());
        }
    }
}
