package com.airline.service;

import com.airline.domain.BookingStatus;
import com.airline.domain.PriorityLevel;
import com.airline.dto.AdminDtos.AnalyticsResponse;
import com.airline.dto.AdminDtos.GroupCount;
import com.airline.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnalyticsService {

    private final BookingRepository bookings;

    public AnalyticsService(BookingRepository bookingRepository) {
        this.bookings = bookingRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse summary() {
        long total = bookings.count();
        long archived = bookings.countByArchived(true);
        BigDecimal average = BigDecimal.valueOf(bookings.averagePrice()).setScale(2, RoundingMode.HALF_UP);
        return new AnalyticsResponse(
                total,
                total - archived,
                archived,
                bookings.countByRefundable(true),
                average,
                passengers(),
                statuses(),
                priorities()
        );
    }

    private List<GroupCount> passengers() {
        List<GroupCount> result = new ArrayList<>();
        for (Object[] row : bookings.countByPassenger()) {
            result.add(new GroupCount(row[0] + " " + row[1], (Long) row[2]));
        }
        return result;
    }

    private List<GroupCount> statuses() {
        List<GroupCount> result = new ArrayList<>();
        for (Object[] row : bookings.countByStatus()) {
            BookingStatus status = (BookingStatus) row[0];
            result.add(new GroupCount(status.getDisplayName(), (Long) row[1]));
        }
        return result;
    }

    private List<GroupCount> priorities() {
        List<GroupCount> result = new ArrayList<>();
        for (Object[] row : bookings.countByPriority()) {
            PriorityLevel priority = (PriorityLevel) row[0];
            result.add(new GroupCount(priority.getDisplayName(), (Long) row[1]));
        }
        return result;
    }
}
