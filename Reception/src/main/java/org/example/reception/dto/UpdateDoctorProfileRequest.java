package org.example.reception.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record UpdateDoctorProfileRequest(
        @NotBlank String fullName,
        @NotBlank String specialization,
        @NotBlank String roomNumber,
        @NotNull @Positive BigDecimal consultationFee,
        @Size(min = 3, max = 3) String currency,
        Boolean active
) {
}
