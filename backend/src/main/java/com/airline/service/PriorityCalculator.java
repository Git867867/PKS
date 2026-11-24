package com.airline.service;

import com.airline.domain.BookingStatus;
import com.airline.domain.PriorityLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class PriorityCalculator {

    private static final BigDecimal HIGH_PRICE = new BigDecimal("10000");
    private static final BigDecimal MEDIUM_PRICE = new BigDecimal("7000");

    private PriorityCalculator() {
    }

    public static PriorityLevel calculate(BookingStatus status, BigDecimal price, LocalDateTime departure, LocalDateTime now) {
        if (status == BookingStatus.COMPLETED || status == BookingStatus.CANCELLED) {
            return PriorityLevel.LOW;
        }
        boolean soonOrPast = !departure.isAfter(now.plusHours(48));
        if (price.compareTo(HIGH_PRICE) >= 0 || soonOrPast) {
            return PriorityLevel.HIGH;
        }
        if (price.compareTo(MEDIUM_PRICE) >= 0) {
            return PriorityLevel.MEDIUM;
        }
        return PriorityLevel.LOW;
    }
}
