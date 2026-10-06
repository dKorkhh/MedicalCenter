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
import org.example.reception.service.AppointmentService;
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
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final BillingServiceAdapter billingServiceAdapter;
    private final IdempotencyService idempotencyService;

    @PostMapping
    public ResponseEntity<AppointmentResponse> scheduleAppointment(
            @RequestHeader(IdempotencyService.HEADER) String idempotencyKey,
            @Valid @RequestBody CreateAppointmentRequest request) {

        IdempotentResult<AppointmentResponse> result = idempotencyService.execute(
                idempotencyKey, request,
                () -> appointmentService.scheduleAppointment(request),
                AppointmentResponse::id,
                appointmentService::getAppointmentById);

        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.CREATED);
        if (result.replayed()) {
            response.header("Idempotent-Replayed", "true");
        }
        return response.body(result.value());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getAppointmentById(@PathVariable String id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> getAppointments(@RequestParam(required = false) String patientId) {
        if (patientId != null && !patientId.isBlank()) {
            return ResponseEntity.ok(appointmentService.getAppointmentsByPatientId(patientId));
        }
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    @GetMapping("/with-invoices")
    public ResponseEntity<List<AppointmentDetailsResponse>> getAppointmentsWithInvoices(
            @RequestParam(required = false) String patientId) {
        return ResponseEntity.ok(appointmentService.getAppointmentDetails(patientId));
    }

    @GetMapping("/invoices/{invoiceId}")
    public ResponseEntity<InvoiceDto> getInvoiceDetails(
            @PathVariable String invoiceId,
            @RequestParam(value = "delay", required = false, defaultValue = "0") long delay) {
        return ResponseEntity.ok(billingServiceAdapter.getInvoice(invoiceId, delay));
    }
}
