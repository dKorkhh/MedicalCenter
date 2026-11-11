package org.example.reception.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record GrpcClientStreamQuoteRequest(
        String patientId,
        List<String> clinicServiceIds,
        List<GrpcServiceQuoteItemDto> items
) {
}
