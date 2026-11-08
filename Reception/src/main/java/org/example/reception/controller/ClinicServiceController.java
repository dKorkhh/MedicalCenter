package org.example.reception.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.reception.dto.ClinicServiceResponse;
import org.example.reception.dto.CreateClinicServiceRequest;
import org.example.reception.dto.UpdateClinicServiceRequest;
import org.example.reception.security.RoleExpressions;
import org.example.reception.service.ClinicServiceCatalogService;
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
@RequestMapping("/api/clinic-services")
@RequiredArgsConstructor
public class ClinicServiceController {

    private final ClinicServiceCatalogService clinicServiceCatalogService;

    @PostMapping
    @PreAuthorize(RoleExpressions.ADMIN)
    public ResponseEntity<ClinicServiceResponse> createClinicService(
            @Valid @RequestBody CreateClinicServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clinicServiceCatalogService.createClinicService(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(RoleExpressions.ADMIN)
    public ResponseEntity<ClinicServiceResponse> updateClinicService(
            @PathVariable String id,
            @Valid @RequestBody UpdateClinicServiceRequest request) {
        return ResponseEntity.ok(clinicServiceCatalogService.updateClinicService(id, request));
    }

    @GetMapping("/{id}")
    @PreAuthorize(RoleExpressions.PATIENT_DOCTOR_OR_ADMIN)
    public ResponseEntity<ClinicServiceResponse> getClinicServiceById(@PathVariable String id) {
        return ResponseEntity.ok(clinicServiceCatalogService.getClinicServiceById(id));
    }

    @GetMapping
    @PreAuthorize(RoleExpressions.PATIENT_DOCTOR_OR_ADMIN)
    public ResponseEntity<List<ClinicServiceResponse>> getClinicServices(
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) Boolean activeOnly) {
        return ResponseEntity.ok(clinicServiceCatalogService.getClinicServices(specialization, activeOnly));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(RoleExpressions.ADMIN)
    public ResponseEntity<Void> deleteClinicServiceById(@PathVariable String id) {
        clinicServiceCatalogService.deleteClinicServiceById(id);
        return ResponseEntity.noContent().build();
    }
}
