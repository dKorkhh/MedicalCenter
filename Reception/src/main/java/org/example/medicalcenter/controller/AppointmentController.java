package org.example.medicalcenter.controller;

import lombok.RequiredArgsConstructor;
import org.example.medicalcenter.dto.AppointmentResponse;
import org.example.medicalcenter.dto.CreateAppointmentRequest;
import org.example.medicalcenter.service.AppointmentService;
import org.example.medicalcenter.client.BillingClient;
import org.example.medicalcenter.client.dto.InvoiceDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final BillingClient billingClient;

    @PostMapping
    public ResponseEntity<AppointmentResponse> scheduleAppointment(@RequestBody CreateAppointmentRequest request) {
        AppointmentResponse scheduledAppointment = appointmentService.scheduleAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(scheduledAppointment);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getAppointmentById(@PathVariable String id) {
        AppointmentResponse appointment = appointmentService.getAppointmentById(id);
        return ResponseEntity.ok(appointment);
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> getAppointments(@RequestParam(required = false) String patientId) {
        if (patientId != null && !patientId.isBlank()) {
            return ResponseEntity.ok(appointmentService.getAppointmentsByPatientId(patientId));
        }
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    @GetMapping("/invoices/{invoiceId}")
    public ResponseEntity<InvoiceDto> getInvoiceDetails(
            @PathVariable String invoiceId,
            @RequestParam(value = "delay", required = false, defaultValue = "0") long delay) {
        InvoiceDto invoice = billingClient.getInvoiceById(invoiceId, delay);
        return ResponseEntity.ok(invoice);
    }
}
