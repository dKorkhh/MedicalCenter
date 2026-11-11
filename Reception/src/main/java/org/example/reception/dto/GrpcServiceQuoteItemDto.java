package org.example.reception.dto;

import lombok.Builder;

@Builder
public record GrpcServiceQuoteItemDto(
        String patientId,
        String serviceId,
        String serviceName,
        String unitPrice,
        int quantity,
        String currency
) {
}
