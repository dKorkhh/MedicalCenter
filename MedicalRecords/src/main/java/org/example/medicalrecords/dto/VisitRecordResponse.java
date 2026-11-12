package org.example.medicalrecords.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record VisitRecordResponse(
        String id,
        String eventId,
        String appointmentId,
        String timeSlotId,
        String patientId,
        String patientFullName,
        String patientEmail,
        String doctorId,
        String doctorFullName,
        String serviceName,
        String diagnosis,
        String prescription,
        String doctorNotes,
        LocalDateTime appointmentDateTime,
        LocalDateTime completedAt,
        LocalDateTime createdAt
) {
}
