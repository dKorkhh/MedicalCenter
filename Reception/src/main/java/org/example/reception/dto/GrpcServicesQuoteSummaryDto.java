package org.example.reception.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record GrpcServicesQuoteSummaryDto(
        String patientId,
        int totalItemsCount,
        int totalUnitsCount,
        String subtotalAmount,
        String discountAmount,
        String finalTotalAmount,
        String currency,
        List<String> includedServices
) {
}
