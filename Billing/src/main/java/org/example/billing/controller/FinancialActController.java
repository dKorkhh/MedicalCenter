package org.example.billing.controller;

import lombok.RequiredArgsConstructor;
import org.example.billing.dto.FinancialActResponse;
import org.example.billing.dto.PaymentSnapshotResponse;
import org.example.billing.security.RoleExpressions;
import org.example.billing.service.FinancialActAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/financial-acts")
@RequiredArgsConstructor
public class FinancialActController {

    private final FinancialActAccessService financialActAccessService;

    @GetMapping("/{id}")
    @PreAuthorize(RoleExpressions.PATIENT_DOCTOR_OR_ADMIN)
    public ResponseEntity<FinancialActResponse> getFinancialActById(@PathVariable String id) {
        return ResponseEntity.ok(financialActAccessService.getFinancialActVisibleToCurrentUser(id));
    }

    @GetMapping("/appointment/{appointmentId}")
    @PreAuthorize(RoleExpressions.PATIENT_DOCTOR_OR_ADMIN)
    public ResponseEntity<FinancialActResponse> getFinancialActByAppointmentId(@PathVariable String appointmentId) {
        return ResponseEntity.ok(financialActAccessService.getFinancialActByAppointmentIdVisibleToCurrentUser(appointmentId));
    }

    @GetMapping("/appointment/{appointmentId}/snapshot")
    @PreAuthorize(RoleExpressions.PATIENT_DOCTOR_OR_ADMIN)
    public ResponseEntity<PaymentSnapshotResponse> getPaymentSnapshotByAppointmentId(@PathVariable String appointmentId) {
        return ResponseEntity.ok(financialActAccessService.getPaymentSnapshotVisibleToCurrentUser(appointmentId));
    }

    @GetMapping
    @PreAuthorize(RoleExpressions.PATIENT_DOCTOR_OR_ADMIN)
    public ResponseEntity<List<FinancialActResponse>> getFinancialActs(
            @RequestParam(required = false) String patientId,
            @RequestParam(required = false) String doctorId) {
        return ResponseEntity.ok(financialActAccessService.getFinancialActsVisibleToCurrentUser(patientId, doctorId));
    }
}
