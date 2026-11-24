package com.airline.service;

import com.airline.domain.BookingStatus;
import com.airline.domain.PriorityLevel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriorityCalculatorTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 11, 11, 12, 0);

    @Test
    void completedIsAlwaysLow() {
        PriorityLevel level = PriorityCalculator.calculate(
                BookingStatus.CANCELLED, new BigDecimal("20000"), now.plusDays(1), now);
        assertEquals(PriorityLevel.LOW, level);
    }

    @Test
    void expensiveTicketIsHigh() {
        PriorityLevel level = PriorityCalculator.calculate(
                BookingStatus.CREATED, new BigDecimal("10000"), now.plusDays(10), now);
        assertEquals(PriorityLevel.HIGH, level);
    }

    @Test
    void mediumPriceIsMedium() {
        PriorityLevel level = PriorityCalculator.calculate(
                BookingStatus.CONFIRMED, new BigDecimal("7000"), now.plusDays(10), now);
        assertEquals(PriorityLevel.MEDIUM, level);
    }

    @Test
    void cheapFutureTicketIsLow() {
        PriorityLevel level = PriorityCalculator.calculate(
                BookingStatus.CREATED, new BigDecimal("1500"), now.plusDays(10), now);
        assertEquals(PriorityLevel.LOW, level);
    }

    @Test
    void departureWithinTwoDaysIsHigh() {
        PriorityLevel level = PriorityCalculator.calculate(
                BookingStatus.CREATED, new BigDecimal("1500"), now.plusHours(5), now);
        assertEquals(PriorityLevel.HIGH, level);
    }
}
