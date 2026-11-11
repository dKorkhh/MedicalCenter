package org.example.reception.controller;

import lombok.RequiredArgsConstructor;
import org.example.reception.dto.GrpcBillingHistoryEventDto;
import org.example.reception.dto.GrpcClientStreamQuoteRequest;
import org.example.reception.dto.GrpcLiveValidationItemRequestDto;
import org.example.reception.dto.GrpcLiveValidationItemResponseDto;
import org.example.reception.dto.GrpcServicesQuoteSummaryDto;
import org.example.reception.dto.GrpcUnaryInvoiceResultDto;
import org.example.reception.grpc.BillingGrpcClientService;
import org.example.reception.security.RoleExpressions;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/grpc/billing")
@RequiredArgsConstructor
public class BillingGrpcController {

    private final BillingGrpcClientService billingGrpcClientService;

    @GetMapping("/unary/invoices/{invoiceId}")
    @PreAuthorize(RoleExpressions.PATIENT_OR_ADMIN)
    public ResponseEntity<GrpcUnaryInvoiceResultDto> getInvoiceUnaryWithRetry(
            @PathVariable String invoiceId,
            @RequestParam(defaultValue = "0") int failFirstAttempts) {
        return ResponseEntity.ok(billingGrpcClientService.fetchInvoiceUnaryWithRetry(invoiceId, failFirstAttempts));
    }

    @GetMapping("/server-stream/patients/{patientId}")
    @PreAuthorize(RoleExpressions.PATIENT_OR_ADMIN)
    public ResponseEntity<List<GrpcBillingHistoryEventDto>> streamPatientBillingHistory(
            @PathVariable String patientId) {
        return ResponseEntity.ok(billingGrpcClientService.streamPatientBillingHistory(patientId));
    }

    @PostMapping("/client-stream/quote")
    @PreAuthorize(RoleExpressions.PATIENT_OR_ADMIN)
    public ResponseEntity<GrpcServicesQuoteSummaryDto> calculateServicesQuoteClientStream(
            @RequestBody GrpcClientStreamQuoteRequest request) {
        return ResponseEntity.ok(billingGrpcClientService.calculateClinicServicesQuote(request));
    }

    @PostMapping("/bidi-stream/validate")
    @PreAuthorize(RoleExpressions.PATIENT_OR_ADMIN)
    public ResponseEntity<List<GrpcLiveValidationItemResponseDto>> validateBillingItemsBidiStream(
            @RequestBody List<GrpcLiveValidationItemRequestDto> items) {
        return ResponseEntity.ok(billingGrpcClientService.runLiveBillingValidation(items));
    }

    @GetMapping("/retry-config")
    @PreAuthorize(RoleExpressions.PATIENT_DOCTOR_OR_ADMIN)
    public ResponseEntity<Map<String, Object>> getGrpcRetryConfiguration() {
        return ResponseEntity.ok(billingGrpcClientService.getRetryConfigurationSummary());
    }
}
