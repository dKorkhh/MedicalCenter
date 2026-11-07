package org.example.medicalrecords.dto;

import lombok.Builder;

@Builder(toBuilder = true)
public record CreateMedicalRecordRequest(
        String patientId,
        String doctorId,
        String appointmentId,
        String diagnosis,
        String prescription,
        String doctorNotes
) {
}
