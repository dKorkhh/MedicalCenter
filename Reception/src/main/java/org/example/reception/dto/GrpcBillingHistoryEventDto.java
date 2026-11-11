package org.example.reception.dto;

import lombok.Builder;

@Builder
public record GrpcBillingHistoryEventDto(
        String recordType,
        String recordId,
        String appointmentId,
        String patientId,
        String amount,
        String currency,
        String status,
        String description,
        String timestamp
) {
}
