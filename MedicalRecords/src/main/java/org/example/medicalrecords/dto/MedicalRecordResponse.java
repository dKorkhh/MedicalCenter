package org.example.medicalrecords.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record MedicalRecordResponse(
        String id,
        String patientId,
        String doctorId,
        String appointmentId,
        String diagnosis,
        String prescription,
        String doctorNotes,
        LocalDateTime recordDate
) {
}
