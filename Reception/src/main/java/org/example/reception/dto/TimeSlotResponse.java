package org.example.reception.dto;

import lombok.Builder;
import org.example.reception.model.TimeSlotStatus;

import java.time.LocalDateTime;

@Builder
public record TimeSlotResponse(
        String id,
        String doctorId,
        String scheduleId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String roomNumber,
        TimeSlotStatus status,
        String heldByPatientId,
        String appointmentId,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
