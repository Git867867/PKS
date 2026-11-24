package com.airline.domain;

import java.util.Map;
import java.util.Set;

public enum BookingStatus {
    CREATED("Создано"),
    CONFIRMED("Подтверждено"),
    COMPLETED("Завершено"),
    CANCELLED("Отменено");

    private final String displayName;

    private static final Map<BookingStatus, Set<BookingStatus>> ALLOWED = Map.of(
            CREATED, Set.of(CONFIRMED, CANCELLED),
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
        return this == target || ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }
}
