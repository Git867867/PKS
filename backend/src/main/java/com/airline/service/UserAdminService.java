package com.airline.service;

import com.airline.config.AppProperties;
import com.airline.domain.RoleName;
import com.airline.domain.UserAccount;
import com.airline.dto.AdminDtos.CreateUserRequest;
import com.airline.dto.AdminDtos.CreatedUserResponse;
import com.airline.dto.AdminDtos.UpdateUserRequest;
import com.airline.dto.PageResponse;
import com.airline.dto.UserResponse;
import com.airline.error.BadRequestException;
import com.airline.error.NotFoundException;
import com.airline.filter.PageRequests;
import com.airline.repository.RefreshSessionRepository;
import com.airline.repository.UserAccountRepository;
import com.airline.security.Auth;
import com.airline.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UserAdminService {

    private static final Set<String> SORT_FIELDS = Set.of("email", "fullName", "createdAt");

    private final UserAccountRepository users;
    private final RefreshSessionRepository sessions;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final AppProperties properties;

    public UserAdminService(UserAccountRepository users, RefreshSessionRepository sessions, PasswordEncoder encoder,
                            AuditService audit, AppProperties properties) {
        this.users = users;
        this.sessions = sessions;
        this.encoder = encoder;
        this.audit = audit;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(String query, int page, int size, String sort, String direction) {
        Specification<UserAccount> spec = (root, cq, cb) -> {
            if (query == null || query.isBlank()) {
                return cb.conjunction();
            }
            String like = "%" + query.trim().toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.like(cb.lower(root.get("email")), like));
            predicates.add(cb.like(cb.lower(root.get("fullName")), like));
            return cb.or(predicates.toArray(Predicate[]::new));
        };
        Page<UserResponse> result = users.findAll(spec,
                        PageRequests.of(page, size, sort, direction, properties.getSecurity().getMaxPageSize(), SORT_FIELDS))
                .map(UserResponse::from);
        return PageResponse.of(result);
    }

    @Transactional
    public CreatedUserResponse create(CreateUserRequest request) {
        String email = request.email().trim();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException("Пользователь с таким email уже есть.");
        }
        String temporary = PasswordPolicy.temporary();
        UserAccount account = new UserAccount();
        account.setEmail(email);
        account.setFullName(request.name().trim());
        account.setPasswordHash(encoder.encode(temporary));
        account.setActive(true);
        account.setMustChangePassword(true);
        account.getRoles().addAll(request.roles());
        users.save(account);
        audit.record(Auth.require().getId(), "USER_CREATED", "UserAccount", account.getId(),
                "Создан пользователь " + account.getEmail());
        return new CreatedUserResponse(UserResponse.from(account), temporary);
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        UserAccount account = require(id);
        CurrentUser current = Auth.require();
        if (account.getId().equals(current.getId()) && !request.active()) {
            throw new BadRequestException("Нельзя отключить свою учетную запись.");
        }
        if (account.getId().equals(current.getId()) && !request.roles().contains(RoleName.ADMIN)) {
            throw new BadRequestException("Нельзя снять с себя роль администратора.");
        }
        String email = request.email().trim();
        if (users.existsByEmailIgnoreCaseAndIdNot(email, account.getId())) {
            throw new BadRequestException("Email уже занят.");
        }
        account.setEmail(email);
        account.setFullName(request.name().trim());
        account.setActive(request.active());
        account.getRoles().clear();
        account.getRoles().addAll(request.roles());
        if (!request.active()) {
            sessions.revokeActiveByUserId(account.getId());
        }
        audit.record(current.getId(), "USER_UPDATED", "UserAccount", account.getId(),
                "Изменен пользователь " + account.getEmail());
        return UserResponse.from(account);
    }

    @Transactional
    public CreatedUserResponse resetPassword(UUID id) {
        UserAccount account = require(id);
        String temporary = PasswordPolicy.temporary();
        account.setPasswordHash(encoder.encode(temporary));
        account.setMustChangePassword(true);
        account.setFailedAttempts(0);
        account.setLockedUntil(null);
        sessions.revokeActiveByUserId(account.getId());
        audit.record(Auth.require().getId(), "PASSWORD_RESET", "UserAccount", account.getId(),
                "Сброшен пароль " + account.getEmail());
        return new CreatedUserResponse(UserResponse.from(account), temporary);
    }

    private UserAccount require(UUID id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("Пользователь не найден."));
    }
}
