package com.airline.controller;

import com.airline.dto.PassengerDtos.ActiveRequest;
import com.airline.dto.PassengerDtos.PassengerRequest;
import com.airline.dto.PassengerDtos.PassengerResponse;
import com.airline.service.PassengerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/passengers")
public class PassengerController {

    private final PassengerService passengers;

    public PassengerController(PassengerService passengers) {
        this.passengers = passengers;
    }

    @GetMapping
    public List<PassengerResponse> list() {
        return passengers.list();
    }

    @PostMapping
    public ResponseEntity<PassengerResponse> create(@Valid @RequestBody PassengerRequest request) {
        return ResponseEntity.status(201).body(passengers.create(request));
    }

    @PutMapping("/{id}")
    public PassengerResponse update(@PathVariable UUID id, @Valid @RequestBody PassengerRequest request) {
        return passengers.update(id, request);
    }

    @PostMapping("/{id}/active")
    public PassengerResponse active(@PathVariable UUID id, @RequestBody ActiveRequest request) {
        return passengers.setActive(id, request.active());
    }
}
