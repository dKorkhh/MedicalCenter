package org.example.reception.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CreateAppointmentRequest(
        @NotBlank String patientId,
        String patientFullName,
        @NotBlank String doctorId,
        String doctorFullName,
        @NotNull LocalDateTime appointmentDateTime,
        String roomNumber,
        String invoiceId
) {
}
