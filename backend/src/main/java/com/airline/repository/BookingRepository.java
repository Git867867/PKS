package com.airline.repository;

import com.airline.domain.Booking;
import com.airline.domain.BookingStatus;
import com.airline.domain.PriorityLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    Optional<Booking> findByPublicId(UUID publicId);

    Optional<Booking> findByBookingCode(String bookingCode);

    boolean existsByBookingCode(String bookingCode);

    long countByArchived(boolean archived);

    long countByRefundable(boolean refundable);

    @Query("select coalesce(avg(b.price), 0) from Booking b")
    double averagePrice();

    @Query("select b.status, count(b) from Booking b group by b.status")
    List<Object[]> countByStatus();

    @Query("select b.priority, count(b) from Booking b group by b.priority")
    List<Object[]> countByPriority();

    @Query("select p.lastName, p.firstName, count(b) from Booking b join b.passenger p group by p.lastName, p.firstName order by count(b) desc")
    List<Object[]> countByPassenger();

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByPriority(PriorityLevel priority);
}
