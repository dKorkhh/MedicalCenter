package org.example.billing.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record InvoiceResponse(
        String id,
        String appointmentId,
        String patientId,
        BigDecimal amount,
        String currency,
        String paymentStatus,
        LocalDateTime issuedAt,
        LocalDateTime paidAt
) {
}
