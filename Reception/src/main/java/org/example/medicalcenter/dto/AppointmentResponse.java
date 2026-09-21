package org.example.medicalcenter.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AppointmentResponse(
        String id,
        String patientId,
        String patientFullName,
        String doctorId,
        String doctorFullName,
        LocalDateTime appointmentDateTime,
        String appointmentStatus,
        String roomNumber,
        LocalDateTime createdAt
) {
}
