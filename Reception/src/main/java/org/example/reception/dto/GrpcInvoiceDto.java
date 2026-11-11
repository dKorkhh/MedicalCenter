package org.example.reception.dto;

import lombok.Builder;

@Builder
public record GrpcInvoiceDto(
        String invoiceId,
        String appointmentId,
        String patientId,
        String amount,
        String currency,
        String paymentStatus,
        int serverAttemptsUsed,
        String processedAt
) {
}
