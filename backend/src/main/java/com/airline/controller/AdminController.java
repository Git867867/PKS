package com.airline.controller;

import com.airline.dto.AdminDtos.AuditResponse;
import com.airline.dto.AdminDtos.CreateUserRequest;
import com.airline.dto.AdminDtos.CreatedUserResponse;
import com.airline.dto.AdminDtos.UpdateUserRequest;
import com.airline.dto.PageResponse;
import com.airline.dto.UserResponse;
import com.airline.service.AuditQueryService;
import com.airline.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserAdminService users;
    private final AuditQueryService audit;

    public AdminController(UserAdminService users, AuditQueryService audit) {
        this.users = users;
        this.audit = audit;
    }

    @GetMapping("/users")
    public PageResponse<UserResponse> users(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "email") String sort,
            @RequestParam(defaultValue = "asc") String direction) {
        return users.list(q, page, size, sort, direction);
    }

    @PostMapping("/users")
    public ResponseEntity<CreatedUserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(201).body(users.create(request));
    }

    @PutMapping("/users/{id}")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return users.update(id, request);
    }

    @PostMapping("/users/{id}/reset-password")
    public CreatedUserResponse reset(@PathVariable UUID id) {
        return users.resetPassword(id);
    }

    @GetMapping("/audit")
    public PageResponse<AuditResponse> audit(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return audit.list(q, page, size, sort, direction);
    }
}
