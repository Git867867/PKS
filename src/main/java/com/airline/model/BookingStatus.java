package com.airline.model;

import java.util.Map;
import java.util.Set;

public enum BookingStatus {
    CREATED("Создано"),
    CONFIRMED("Подтверждено"),
    COMPLETED("Завершено"),
    CANCELLED("Отменено");

    private final String displayName;

    private static final Map<BookingStatus, Set<BookingStatus>> ALLOWED_TRANSITIONS = Map.of(
            CREATED,   Set.of(CONFIRMED, CANCELLED),
            CONFIRMED, Set.of(COMPLETED, CANCELLED),
            COMPLETED, Set.of(),
            CANCELLED, Set.of()
    );

    BookingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean canTransitionTo(BookingStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public static BookingStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Статус не может быть пустым.");
        }
        try {
            return BookingStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Некорректный статус: '" + value + "'. Допустимые значения: CREATED, CONFIRMED, COMPLETED, CANCELLED.");
        }
    }

    @Override
    public String toString() {
        return name() + " (" + displayName + ")";
    }
}