package com.airline.service;

import com.airline.domain.AuditEvent;
import com.airline.repository.AuditEventRepository;
import com.airline.repository.UserAccountRepository;
import com.airline.security.Correlation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditEventRepository events;
    private final UserAccountRepository users;

    public AuditService(AuditEventRepository events, UserAccountRepository users) {
        this.events = events;
        this.users = users;
    }

    @Transactional
    public void record(UUID userId, String eventType, String entityType, UUID entityId, String description) {
        AuditEvent event = new AuditEvent();
        event.setId(UUID.randomUUID());
        if (userId != null) {
            users.findById(userId).ifPresent(event::setUser);
        }
        event.setEventType(eventType);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setDescription(shorten(description));
        event.setCorrelationId(Correlation.get());
        event.setCreatedAt(LocalDateTime.now());
        events.save(event);
    }

    private String shorten(String description) {
        if (description == null || description.isBlank()) {
            return "—";
        }
        return description.length() <= 300 ? description : description.substring(0, 300);
    }
}
