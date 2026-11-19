package org.example.medicalrecords.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder(toBuilder = true)
public record CreateMedicalRecordRequest(
        @NotBlank String patientId,
        String doctorId,
        @NotBlank String appointmentId,
        @NotBlank String diagnosis,
        String prescription,
        String doctorNotes
) {
}
