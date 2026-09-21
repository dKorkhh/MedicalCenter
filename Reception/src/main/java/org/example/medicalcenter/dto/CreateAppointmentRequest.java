package org.example.medicalcenter.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record CreateAppointmentRequest(
        String patientId,
        String patientFullName,
        String doctorId,
        String doctorFullName,
        LocalDateTime appointmentDateTime,
        String roomNumber
) {
}
