package org.example.reception.dto;

import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Builder
public record DoctorScheduleResponse(
        String id,
        String doctorId,
        Set<DayOfWeek> daysOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer slotDurationMinutes,
        String roomNumber,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
