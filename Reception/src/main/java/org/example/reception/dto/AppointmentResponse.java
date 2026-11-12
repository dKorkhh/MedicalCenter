package org.example.reception.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AppointmentResponse(
        String id,
        String patientId,
        String patientFullName,
        String doctorId,
        String doctorFullName,
        String timeSlotId,
        LocalDateTime appointmentDateTime,
        String appointmentStatus,
        String roomNumber,
        String invoiceId,
        String cancelledBy,
        String cancellationReason,
        LocalDateTime cancelledAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
}
