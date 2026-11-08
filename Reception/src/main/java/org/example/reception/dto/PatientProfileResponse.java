package org.example.reception.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record PatientProfileResponse(
        String id,
        String fullName,
        String email,
        String phone,
        LocalDate birthDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
