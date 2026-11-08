package org.example.reception.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.reception.dto.DoctorScheduleResponse;
import org.example.reception.dto.GenerateScheduleSlotsRequest;
import org.example.reception.dto.TimeSlotResponse;
import org.example.reception.dto.UpsertDoctorScheduleRequest;
import org.example.reception.security.RoleExpressions;
import org.example.reception.service.DoctorScheduleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class DoctorScheduleController {

    private final DoctorScheduleService doctorScheduleService;

    @PutMapping
    @PreAuthorize(RoleExpressions.ADMIN)
    public ResponseEntity<DoctorScheduleResponse> upsertDoctorSchedule(
            @Valid @RequestBody UpsertDoctorScheduleRequest request) {
        return ResponseEntity.ok(doctorScheduleService.upsertDoctorSchedule(request));
    }

    @PostMapping("/generate")
    @PreAuthorize(RoleExpressions.ADMIN)
    public ResponseEntity<List<TimeSlotResponse>> generateTimeSlots(
            @Valid @RequestBody GenerateScheduleSlotsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorScheduleService.generateTimeSlots(request));
    }

    @GetMapping("/doctor/{doctorId}")
    @PreAuthorize(RoleExpressions.PATIENT_DOCTOR_OR_ADMIN)
    public ResponseEntity<DoctorScheduleResponse> getDoctorScheduleByDoctorId(@PathVariable String doctorId) {
        return ResponseEntity.ok(doctorScheduleService.getDoctorScheduleByDoctorId(doctorId));
    }

    @GetMapping
    @PreAuthorize(RoleExpressions.PATIENT_DOCTOR_OR_ADMIN)
    public ResponseEntity<List<DoctorScheduleResponse>> getAllDoctorSchedules() {
        return ResponseEntity.ok(doctorScheduleService.getAllDoctorSchedules());
    }
}
