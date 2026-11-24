package com.airline.filter;

import com.airline.domain.Booking;
import com.airline.domain.Passenger;
import com.airline.domain.Tag;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class BookingSpecifications {

    private BookingSpecifications() {
    }

    public static Specification<Booking> byFilter(BookingFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter.query() != null && !filter.query().isBlank()) {
                String like = "%" + escape(filter.query().trim().toLowerCase()) + "%";
                Join<Booking, Passenger> passenger = root.join("passenger");
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("flightNumber")), like, '\\'),
                        cb.like(cb.lower(root.get("bookingCode")), like, '\\'),
                        cb.like(cb.lower(root.get("departureCity")), like, '\\'),
                        cb.like(cb.lower(root.get("arrivalCity")), like, '\\'),
                        cb.like(cb.lower(passenger.get("firstName")), like, '\\'),
                        cb.like(cb.lower(passenger.get("lastName")), like, '\\')
                ));
            }
            if (filter.passengerId() != null) {
                predicates.add(cb.equal(root.get("passenger").get("publicId"), filter.passengerId()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.refundable() != null) {
                predicates.add(cb.equal(root.get("refundable"), filter.refundable()));
            }
            if (filter.priority() != null) {
                predicates.add(cb.equal(root.get("priority"), filter.priority()));
            }
            if (filter.tag() != null && !filter.tag().isBlank()) {
                if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                    query.distinct(true);
                }
                Join<Booking, Tag> tags = root.join("tags");
                predicates.add(cb.equal(cb.lower(tags.get("name")), filter.tag().trim().toLowerCase()));
            }
            String view = filter.view() == null ? "active" : filter.view();
            if ("archived".equalsIgnoreCase(view)) {
                predicates.add(cb.isTrue(root.get("archived")));
            } else if (!"all".equalsIgnoreCase(view)) {
                predicates.add(cb.isFalse(root.get("archived")));
            }
            if (filter.price() != null && filter.priceOp() != null) {
                predicates.add(switch (filter.priceOp()) {
                    case GT -> cb.greaterThan(root.get("price"), filter.price());
                    case GTE -> cb.greaterThanOrEqualTo(root.get("price"), filter.price());
                    case LT -> cb.lessThan(root.get("price"), filter.price());
                    case LTE -> cb.lessThanOrEqualTo(root.get("price"), filter.price());
                    case EQ -> cb.equal(root.get("price"), filter.price());
                });
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
