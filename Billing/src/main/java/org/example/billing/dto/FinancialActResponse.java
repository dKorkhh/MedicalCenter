package org.example.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FinancialActResponse(
        String id,
        String actNumber,
        String appointmentId,
        String patientId,
        String patientFullName,
        String doctorId,
        String doctorFullName,
        String invoiceId,
        String serviceName,
        BigDecimal amount,
        String currency,
        String status,
        LocalDateTime issuedAt,
        LocalDateTime closedAt
) {
}
