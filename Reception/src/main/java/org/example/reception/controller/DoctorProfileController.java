package org.example.reception.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.reception.dto.CreateDoctorProfileRequest;
import org.example.reception.dto.DoctorProfileResponse;
import org.example.reception.dto.UpdateDoctorProfileRequest;
import org.example.reception.security.RoleExpressions;
import org.example.reception.service.DoctorProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorProfileController {

    private final DoctorProfileService doctorProfileService;

    @PostMapping
    @PreAuthorize(RoleExpressions.ADMIN)
    public ResponseEntity<DoctorProfileResponse> createDoctorProfile(
            @Valid @RequestBody CreateDoctorProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorProfileService.createDoctorProfile(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(RoleExpressions.ADMIN)
    public ResponseEntity<DoctorProfileResponse> updateDoctorProfile(
            @PathVariable String id,
            @Valid @RequestBody UpdateDoctorProfileRequest request) {
        return ResponseEntity.ok(doctorProfileService.updateDoctorProfile(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize(RoleExpressions.PATIENT_DOCTOR_OR_ADMIN)
    public ResponseEntity<DoctorProfileResponse> getDoctorProfileById(@PathVariable String id) {
        return ResponseEntity.ok(doctorProfileService.getDoctorProfileById(id));
    }

    @GetMapping
    @PreAuthorize(RoleExpressions.PATIENT_DOCTOR_OR_ADMIN)
    public ResponseEntity<List<DoctorProfileResponse>> getDoctorProfiles(
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) Boolean activeOnly) {
        return ResponseEntity.ok(doctorProfileService.getDoctorProfiles(specialization, activeOnly));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(RoleExpressions.ADMIN)
    public ResponseEntity<Void> deleteDoctorProfileById(@PathVariable String id) {
        doctorProfileService.deleteDoctorProfileById(id);
        return ResponseEntity.noContent().build();
    }
}
