package org.example.billing.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.billing.dto.CreateInvoiceRequest;
import org.example.billing.dto.InvoiceResponse;
import org.example.billing.idempotency.IdempotencyService;
import org.example.billing.idempotency.IdempotentResult;
import org.example.billing.security.RoleExpressions;
import org.example.billing.service.InvoiceAccessService;
import org.example.billing.service.InvoiceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private static final long MAX_SIMULATED_DELAY_MS = 10_000;

    private final InvoiceService invoiceService;
    private final InvoiceAccessService invoiceAccessService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    @PreAuthorize(RoleExpressions.ADMIN)
    public ResponseEntity<InvoiceResponse> issueInvoice(
            @RequestHeader(IdempotencyService.HEADER) String idempotencyKey,
            @Valid @RequestBody CreateInvoiceRequest request) {

        IdempotentResult<InvoiceResponse> result = idempotencyService.execute(
                idempotencyKey, request,
                () -> invoiceService.issueInvoice(request),
                InvoiceResponse::id,
                invoiceService::getInvoiceById);

        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.CREATED);
        if (result.replayed()) {
            response.header("Idempotent-Replayed", "true");
        }
        return response.body(result.value());
    }

    @GetMapping("/{id}")
    @PreAuthorize(RoleExpressions.PATIENT_OR_ADMIN)
    public ResponseEntity<InvoiceResponse> getInvoiceById(
            @PathVariable String id,
            @RequestParam(value = "delay", required = false, defaultValue = "0") long delay) throws InterruptedException {
        if (delay > 0) {
            Thread.sleep(Math.min(delay, MAX_SIMULATED_DELAY_MS));
        }
        return ResponseEntity.ok(invoiceAccessService.getInvoiceVisibleToCurrentUser(id));
    }

    @GetMapping
    @PreAuthorize(RoleExpressions.PATIENT_OR_ADMIN)
    public ResponseEntity<List<InvoiceResponse>> getInvoices(@RequestParam(required = false) String patientId) {
        return ResponseEntity.ok(invoiceAccessService.getInvoicesVisibleToCurrentUser(patientId));
    }

    @PostMapping("/batch")
    @PreAuthorize(RoleExpressions.PATIENT_OR_ADMIN)
    public ResponseEntity<List<InvoiceResponse>> getInvoicesBatch(@RequestBody List<String> ids) {
        return ResponseEntity.ok(invoiceAccessService.getInvoicesBatchVisibleToCurrentUser(ids));
    }
}
