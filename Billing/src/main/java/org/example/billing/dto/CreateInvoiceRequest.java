package org.example.billing.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CreateInvoiceRequest(
        String appointmentId,
        String patientId,
        BigDecimal amount,
        String currency
) {
}
