package org.example.billing.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.billing.dto.CreateInvoiceRequest;
import org.example.billing.dto.InvoiceResponse;
import org.example.billing.idempotency.IdempotencyService;
import org.example.billing.idempotency.IdempotentResult;
import org.example.billing.service.InvoiceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final IdempotencyService idempotencyService;

    @PostMapping
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
    public ResponseEntity<InvoiceResponse> getInvoiceById(
            @PathVariable String id,
            @RequestParam(value = "delay", required = false, defaultValue = "0") long delay) throws InterruptedException {
        if (delay > 0) {
            Thread.sleep(Math.min(delay, MAX_SIMULATED_DELAY_MS));
        }
        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
    }

    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> getInvoices(@RequestParam(required = false) String patientId) {
        if (patientId != null && !patientId.isBlank()) {
            return ResponseEntity.ok(invoiceService.getInvoicesByPatientId(patientId));
        }
        return ResponseEntity.ok(invoiceService.getAllInvoices());
    }

    @PostMapping("/batch")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesBatch(@RequestBody List<String> ids) {
        return ResponseEntity.ok(invoiceService.getInvoicesByIds(ids));
    }
}
