package org.example.reception.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CreateClinicServiceRequest(
        @NotBlank String name,
        String description,
        @NotBlank String specialization,
        @NotNull @Positive BigDecimal price,
        @Size(min = 3, max = 3) String currency,
        @NotNull @Positive Integer durationMinutes,
        Boolean active
) {
}
