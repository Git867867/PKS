package com.airline.filter;

import com.airline.error.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PageRequestsTest {

    private static final Set<String> ALLOWED = Set.of("departureDate", "price", "status", "createdAt");

    @Test
    void acceptsWhitelistedSort() {
        var page = PageRequests.of(0, 20, "price", "desc", 100, ALLOWED);
        assertEquals("price: DESC", page.getSort().toString());
    }

    @Test
    void rejectsUnknownSortField() {
        assertThrows(BadRequestException.class,
                () -> PageRequests.of(0, 20, "flightNumber; drop table", "asc", 100, ALLOWED));
    }

    @Test
    void rejectsTooLargePage() {
        assertThrows(BadRequestException.class,
                () -> PageRequests.of(0, 500, "status", "asc", 100, ALLOWED));
    }

    @Test
    void rejectsUnknownPriceOperation() {
        assertThrows(BadRequestException.class, () -> PriceComparison.parse("LIKE"));
    }
}
