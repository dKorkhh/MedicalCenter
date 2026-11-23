package org.example.medicalrecords.controller;

import lombok.RequiredArgsConstructor;
import org.example.medicalrecords.dto.VisitRecordResponse;
import org.example.medicalrecords.security.CurrentUserProvider;
import org.example.medicalrecords.security.RoleExpressions;
import org.example.medicalrecords.service.VisitRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/medical-records/visits")
@RequiredArgsConstructor
public class VisitRecordController {

    private final VisitRecordService visitRecordService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/appointment/{appointmentId}")
    @PreAuthorize(RoleExpressions.PATIENT_DOCTOR_OR_ADMIN)
    public ResponseEntity<VisitRecordResponse> getVisitRecordByAppointmentId(@PathVariable String appointmentId) {
        VisitRecordResponse visitRecordResponse = visitRecordService.getVisitRecordByAppointmentId(appointmentId);
        verifyCanViewVisitRecord(visitRecordResponse);
        return ResponseEntity.ok(visitRecordResponse);
    }

    @GetMapping
    @PreAuthorize(RoleExpressions.PATIENT_DOCTOR_OR_ADMIN)
    public ResponseEntity<List<VisitRecordResponse>> getVisitRecords(
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String doctorId) {
        if (currentUserProvider.isAdmin()) {
            if (patientId != null && !patientId.isBlank()) {
                return ResponseEntity.ok(visitRecordService.getVisitRecordsByPatientId(patientId));
            }
            if (doctorId != null && !doctorId.isBlank()) {
                return ResponseEntity.ok(visitRecordService.getVisitRecordsByDoctorId(doctorId));
            }
            return ResponseEntity.ok(List.of());
        }
        if (currentUserProvider.isDoctor()) {
            return ResponseEntity.ok(visitRecordService.getVisitRecordsByDoctorId(currentUserProvider.currentUserId()));
        }
        return ResponseEntity.ok(visitRecordService.getVisitRecordsByPatientId(currentUserProvider.currentUserId()));
    }

    private void verifyCanViewVisitRecord(VisitRecordResponse visitRecordResponse) {
        if (currentUserProvider.isAdmin()) {
            return;
        }
        String currentUserId = currentUserProvider.currentUserId();
        if (currentUserProvider.isDoctor() && currentUserId.equals(visitRecordResponse.doctorId())) {
            return;
        }
        if (currentUserId.equals(visitRecordResponse.patientId())) {
            return;
        }
        throw new AccessDeniedException("Access denied to visit record for appointment: " + visitRecordResponse.appointmentId());
    }
}
