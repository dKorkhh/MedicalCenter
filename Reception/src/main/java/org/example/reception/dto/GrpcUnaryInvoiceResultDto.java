package org.example.reception.dto;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record GrpcUnaryInvoiceResultDto(
        String requestId,
        GrpcInvoiceDto invoice,
        int requestedSimulatedFailures,
        int totalAttemptsExecuted,
        Map<String, Object> retryPolicyConfiguration,
        List<String> retryExecutionLogs
) {
}
