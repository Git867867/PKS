package com.airline.filter;

import com.airline.domain.BookingStatus;
import com.airline.domain.PriorityLevel;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingFilter(
        String query,
        UUID passengerId,
        BookingStatus status,
        Boolean refundable,
        PriorityLevel priority,
        String tag,
        String view,
        PriceComparison priceOp,
        BigDecimal price
) {
}
