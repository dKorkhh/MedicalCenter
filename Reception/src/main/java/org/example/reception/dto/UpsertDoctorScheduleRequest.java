package org.example.reception.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

@Builder
public record UpsertDoctorScheduleRequest(
        @NotBlank String doctorId,
        @NotEmpty Set<DayOfWeek> daysOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull @Positive Integer slotDurationMinutes,
        String roomNumber,
        Boolean active
) {
}
