package org.example.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundResponse(
        String id,
        String eventId,
        String appointmentId,
        String invoiceId,
        String patientId,
        BigDecimal amount,
        String currency,
        String status,
        String cancelledBy,
        String reason,
        LocalDateTime processedAt
) {
}
