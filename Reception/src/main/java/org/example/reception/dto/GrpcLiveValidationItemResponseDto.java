package org.example.reception.dto;

import lombok.Builder;

@Builder
public record GrpcLiveValidationItemResponseDto(
        String stepId,
        String invoiceId,
        String serviceName,
        boolean valid,
        String validationStatus,
        String runningTotal,
        String currency,
        String message
) {
}
