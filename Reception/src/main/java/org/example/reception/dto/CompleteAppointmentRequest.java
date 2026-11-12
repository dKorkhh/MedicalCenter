package org.example.reception.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CompleteAppointmentRequest(
        @NotBlank String diagnosis,
        String prescription,
        String doctorNotes,
        String serviceName,
        BigDecimal amount,
        String currency
) {
}
