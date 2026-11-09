package org.example.billing.controller;

import lombok.RequiredArgsConstructor;
import org.example.billing.dto.RefundResponse;
import org.example.billing.security.RoleExpressions;
import org.example.billing.service.RefundAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundAccessService refundAccessService;

    @GetMapping("/{id}")
    @PreAuthorize(RoleExpressions.PATIENT_OR_ADMIN)
    public ResponseEntity<RefundResponse> getRefundById(@PathVariable String id) {
        return ResponseEntity.ok(refundAccessService.getRefundVisibleToCurrentUser(id));
    }

    @GetMapping
    @PreAuthorize(RoleExpressions.PATIENT_OR_ADMIN)
    public ResponseEntity<List<RefundResponse>> getRefunds(@RequestParam(required = false) String patientId) {
        return ResponseEntity.ok(refundAccessService.getRefundsVisibleToCurrentUser(patientId));
    }
}
