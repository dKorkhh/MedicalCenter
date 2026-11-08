package org.example.reception.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.reception.dto.PatientProfileResponse;
import org.example.reception.dto.UpsertPatientProfileRequest;
import org.example.reception.security.RoleExpressions;
import org.example.reception.service.PatientProfileAccessService;
import org.example.reception.service.PatientProfileService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientProfileController {

    private final PatientProfileService patientProfileService;
    private final PatientProfileAccessService patientProfileAccessService;

    @GetMapping("/me")
    @PreAuthorize(RoleExpressions.PATIENT_OR_ADMIN)
    public ResponseEntity<PatientProfileResponse> getCurrentPatientProfile() {
        return ResponseEntity.ok(patientProfileAccessService.getCurrentPatientProfile());
    }

    @PutMapping("/me")
    @PreAuthorize(RoleExpressions.PATIENT_OR_ADMIN)
    public ResponseEntity<PatientProfileResponse> upsertCurrentPatientProfile(
            @Valid @RequestBody UpsertPatientProfileRequest request) {
        return ResponseEntity.ok(patientProfileAccessService.upsertCurrentPatientProfile(request));
    }

    @PostMapping
    @PreAuthorize(RoleExpressions.ADMIN)
    public ResponseEntity<PatientProfileResponse> createPatientProfile(
            @Valid @RequestBody UpsertPatientProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientProfileService.createPatientProfile(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(RoleExpressions.PATIENT_OR_ADMIN)
    public ResponseEntity<PatientProfileResponse> updatePatientProfile(
            @PathVariable String id,
            @Valid @RequestBody UpsertPatientProfileRequest request) {
        return ResponseEntity.ok(patientProfileAccessService.updatePatientProfileVisibleToCurrentUser(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize(RoleExpressions.PATIENT_DOCTOR_OR_ADMIN)
    public ResponseEntity<PatientProfileResponse> getPatientProfileById(@PathVariable String id) {
        return ResponseEntity.ok(patientProfileAccessService.getPatientProfileVisibleToCurrentUser(id));
    }

    @GetMapping
    @PreAuthorize(RoleExpressions.PATIENT_DOCTOR_OR_ADMIN)
    public ResponseEntity<List<PatientProfileResponse>> getAllPatientProfiles() {
        return ResponseEntity.ok(patientProfileAccessService.getAllPatientProfilesVisibleToCurrentUser());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(RoleExpressions.ADMIN)
    public ResponseEntity<Void> deletePatientProfileById(@PathVariable String id) {
        patientProfileService.deletePatientProfileById(id);
        return ResponseEntity.noContent().build();
    }
}
