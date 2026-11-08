package org.example.reception.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

@Builder
public record GenerateScheduleSlotsRequest(
        @NotBlank String doctorId,
        Set<DayOfWeek> daysOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        @Positive Integer slotDurationMinutes,
        String roomNumber,
        LocalDate startDate,
        @Positive Integer daysAhead
) {
}
