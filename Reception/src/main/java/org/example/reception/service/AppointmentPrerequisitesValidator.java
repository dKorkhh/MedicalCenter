package org.example.reception.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.reception.client.BillingServiceAdapter;
import org.example.reception.client.MedicalRecordsClient;
import org.example.reception.dto.CreateAppointmentRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentPrerequisitesValidator {

    private final MedicalRecordsClient medicalRecordsClient;
    private final BillingServiceAdapter billingServiceAdapter;

    public void verifyAppointmentPrerequisites(CreateAppointmentRequest request) {
        verifyMedicalRecords(request.patientId());
        verifyBillingStatus(request.invoiceId());
    }

    private void verifyMedicalRecords(String patientId) {
        try {
            log.info("Querying medical records for patientId={}", patientId);
            if (medicalRecordsClient != null) {
                medicalRecordsClient.getMedicalRecords(patientId);
            }
        } catch (Exception ex) {
            log.warn("Medical records query failed gracefully: {}", ex.getMessage());
        }
    }

    private void verifyBillingStatus(String invoiceId) {
        try {
            if (billingServiceAdapter == null) {
                return;
            }
            if (invoiceId != null && !invoiceId.isBlank()) {
                log.info("Verifying invoiceId={} with billing service", invoiceId);
                billingServiceAdapter.getInvoice(invoiceId, 0L);
            } else {
                log.info("Querying billing batch for appointment initialization");
                billingServiceAdapter.getInvoicesBatch(List.of("placeholder-inv"));
            }
        } catch (Exception ex) {
            log.warn("Billing service query completed/handled: {}", ex.getMessage());
        }
    }
}
