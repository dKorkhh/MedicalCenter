package org.example.reception.controller;

import lombok.RequiredArgsConstructor;
import org.example.reception.dto.TimeSlotResponse;
import org.example.reception.model.TimeSlotStatus;
import org.example.reception.security.CurrentUserProvider;
import org.example.reception.security.RoleExpressions;
import org.example.reception.service.TimeSlotReservationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/time-slots")
@RequiredArgsConstructor
public class TimeSlotController {

    private final TimeSlotReservationService timeSlotReservationService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    @PreAuthorize(RoleExpressions.PATIENT_DOCTOR_OR_ADMIN)
    public ResponseEntity<List<TimeSlotResponse>> getTimeSlots(
            @RequestParam String doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) TimeSlotStatus status) {
        return ResponseEntity.ok(timeSlotReservationService.getTimeSlots(doctorId, date, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize(RoleExpressions.PATIENT_DOCTOR_OR_ADMIN)
    public ResponseEntity<TimeSlotResponse> getTimeSlotById(@PathVariable String id) {
        return ResponseEntity.ok(timeSlotReservationService.getTimeSlotById(id));
    }

    @PostMapping("/{id}/hold")
    @PreAuthorize(RoleExpressions.PATIENT_OR_ADMIN)
    public ResponseEntity<TimeSlotResponse> holdTimeSlot(@PathVariable String id) {
        return ResponseEntity.ok(timeSlotReservationService.holdTimeSlot(id, currentUserProvider.currentUserId()));
    }

    @PostMapping("/{id}/release")
    @PreAuthorize(RoleExpressions.PATIENT_OR_ADMIN)
    public ResponseEntity<TimeSlotResponse> releaseTimeSlot(@PathVariable String id) {
        return ResponseEntity.ok(timeSlotReservationService.releaseTimeSlot(
                id, currentUserProvider.currentUserId(), currentUserProvider.isAdmin()));
    }
}
