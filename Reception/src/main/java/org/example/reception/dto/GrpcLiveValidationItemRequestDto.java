package org.example.reception.dto;

import lombok.Builder;

@Builder
public record GrpcLiveValidationItemRequestDto(
        String stepId,
        String invoiceId,
        String serviceName,
        String requestedAmount,
        String currency
) {
}
