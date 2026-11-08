package org.example.reception.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record DoctorProfileResponse(
        String id,
        String fullName,
        String specialization,
        String roomNumber,
        BigDecimal consultationFee,
        String currency,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
