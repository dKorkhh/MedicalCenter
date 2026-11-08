package org.example.reception.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record ClinicServiceResponse(
        String id,
        String name,
        String description,
        String specialization,
        BigDecimal price,
        String currency,
        Integer durationMinutes,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
