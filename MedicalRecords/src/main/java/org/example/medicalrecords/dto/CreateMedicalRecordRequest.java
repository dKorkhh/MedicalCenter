package org.example.medicalrecords.dto;

import lombok.Builder;

@Builder
public record CreateMedicalRecordRequest(
        String patientId,
        String doctorId,
        String appointmentId,
        String diagnosis,
        String prescription,
        String doctorNotes
) {
}
