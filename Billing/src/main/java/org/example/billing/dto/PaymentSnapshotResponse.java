package org.example.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentSnapshotResponse(
        String id,
        String appointmentId,
        String invoiceId,
        String patientId,
        BigDecimal amount,
        String currency,
        String snapshotStatus,
        String initialPaymentStatus,
        String finalPaymentStatus,
        LocalDateTime closedAt
) {
}
