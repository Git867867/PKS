package com.airline.service;

import com.airline.config.AppProperties;
import com.airline.domain.AuditEvent;
import com.airline.dto.AdminDtos.AuditResponse;
import com.airline.dto.PageResponse;
import com.airline.filter.PageRequests;
import com.airline.repository.AuditEventRepository;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class AuditQueryService {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "eventType");

    private final AuditEventRepository events;
    private final AppProperties properties;

    public AuditQueryService(AuditEventRepository events, AppProperties properties) {
        this.events = events;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditResponse> list(String query, int page, int size, String sort, String direction) {
        Specification<AuditEvent> spec = (root, cq, cb) -> {
            if (query == null || query.isBlank()) {
                return cb.conjunction();
            }
            String like = "%" + query.trim().toLowerCase() + "%";
            var user = root.join("user", JoinType.LEFT);
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.like(cb.lower(root.get("eventType")), like));
            predicates.add(cb.like(cb.lower(root.get("description")), like));
            predicates.add(cb.like(cb.lower(user.get("email")), like));
            return cb.or(predicates.toArray(Predicate[]::new));
        };
        Page<AuditResponse> result = events.findAll(spec,
                        PageRequests.of(page, size, sort, direction, properties.getSecurity().getMaxPageSize(), SORT_FIELDS))
                .map(AuditResponse::from);
        return PageResponse.of(result);
    }
}
