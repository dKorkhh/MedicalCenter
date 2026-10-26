package org.example.reception.client.dto;

import java.time.LocalDateTime;

public record MedicalRecordDto(
        String id,
        String patientId,
        String doctorId,
        String diagnosis,
        String treatment,
        String notes,
        LocalDateTime createdAt
) {}
