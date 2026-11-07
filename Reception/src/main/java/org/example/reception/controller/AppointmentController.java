package org.example.reception.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.reception.client.BillingServiceAdapter;
import org.example.reception.client.dto.InvoiceDto;
import org.example.reception.dto.AppointmentDetailsResponse;
import org.example.reception.dto.AppointmentResponse;
import org.example.reception.dto.CreateAppointmentRequest;
import org.example.reception.idempotency.IdempotencyService;
import org.example.reception.idempotency.IdempotentResult;
import org.example.reception.metrics.AppointmentMetricsService;
import org.example.reception.outbox.OutboxMessage;
import org.example.reception.outbox.OutboxMessageRepository;
import org.example.reception.outbox.OutboxStatus;
import org.example.reception.security.RoleExpressions;
import org.example.reception.service.AppointmentAccessService;
import org.example.reception.service.AppointmentService;
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
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentAccessService appointmentAccessService;
    private final BillingServiceAdapter billingServiceAdapter;
    private final IdempotencyService idempotencyService;
    private final OutboxMessageRepository outboxMessageRepository;
    private final AppointmentMetricsService appointmentMetricsService;

    @GetMapping("/outbox")
    @PreAuthorize(RoleExpressions.ADMIN)
    public ResponseEntity<List<OutboxMessage>> getOutboxMessages(@RequestParam(required = false) OutboxStatus status) {
        if (status != null) {
            return ResponseEntity.ok(outboxMessageRepository.findByStatus(status));
        }
        return ResponseEntity.ok(outboxMessageRepository.findAll());
    }

    @PostMapping
    @PreAuthorize(RoleExpressions.PATIENT_OR_ADMIN)
    public ResponseEntity<AppointmentResponse> scheduleAppointment(
            @RequestHeader(IdempotencyService.HEADER) String idempotencyKey,
            @Valid @RequestBody CreateAppointmentRequest request) {

        CreateAppointmentRequest ownedRequest = appointmentAccessService.bindRequestToCurrentPatient(request);
        IdempotentResult<AppointmentResponse> result = idempotencyService.execute(
                idempotencyKey, ownedRequest,
                () -> appointmentService.scheduleAppointment(ownedRequest),
                AppointmentResponse::id,
                appointmentService::getAppointmentById);

        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.CREATED);
        if (result.replayed()) {
            response.header("Idempotent-Replayed", "true");
        }
        appointmentMetricsService.recordAppointment();
        return response.body(result.value());
    }

    @GetMapping("/{id}")
    @PreAuthorize(RoleExpressions.PATIENT_DOCTOR_OR_ADMIN)
    public ResponseEntity<AppointmentResponse> getAppointmentById(@PathVariable String id) {
        return ResponseEntity.ok(appointmentAccessService.getAppointmentVisibleToCurrentUser(id));
    }

    @GetMapping
    @PreAuthorize(RoleExpressions.PATIENT_DOCTOR_OR_ADMIN)
    public ResponseEntity<List<AppointmentResponse>> getAppointments(@RequestParam(required = false) String patientId) {
        return ResponseEntity.ok(appointmentAccessService.getAppointmentsVisibleToCurrentUser(patientId));
    }

    @GetMapping("/with-invoices")
    @PreAuthorize(RoleExpressions.PATIENT_OR_ADMIN)
    public ResponseEntity<List<AppointmentDetailsResponse>> getAppointmentsWithInvoices(
            @RequestParam(required = false) String patientId) {
        return ResponseEntity.ok(appointmentAccessService.getAppointmentDetailsVisibleToCurrentUser(patientId));
    }

    @GetMapping("/invoices/{invoiceId}")
    @PreAuthorize(RoleExpressions.PATIENT_OR_ADMIN)
    public ResponseEntity<InvoiceDto> getInvoiceDetails(
            @PathVariable String invoiceId,
            @RequestParam(value = "delay", required = false, defaultValue = "0") long delay) {
        return ResponseEntity.ok(billingServiceAdapter.getInvoice(invoiceId, delay));
    }
}
