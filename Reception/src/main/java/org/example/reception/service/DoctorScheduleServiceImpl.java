package org.example.reception.service;

import lombok.RequiredArgsConstructor;
import org.example.reception.dto.DoctorScheduleResponse;
import org.example.reception.dto.GenerateScheduleSlotsRequest;
import org.example.reception.dto.TimeSlotResponse;
import org.example.reception.dto.UpsertDoctorScheduleRequest;
import org.example.reception.exception.ApiProblemException;
import org.example.reception.mapper.DoctorScheduleMapper;
import org.example.reception.mapper.TimeSlotMapper;
import org.example.reception.model.DoctorSchedule;
import org.example.reception.model.TimeSlot;
import org.example.reception.model.TimeSlotStatus;
import org.example.reception.repository.DoctorScheduleRepository;
import org.example.reception.repository.TimeSlotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorScheduleServiceImpl implements DoctorScheduleService {

    private static final int DEFAULT_DAYS_AHEAD = 30;
    private static final int DEFAULT_SLOT_DURATION_MINUTES = 30;

    private final DoctorScheduleRepository doctorScheduleRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final DoctorScheduleMapper doctorScheduleMapper;
    private final TimeSlotMapper timeSlotMapper;

    @Override
    public DoctorScheduleResponse upsertDoctorSchedule(UpsertDoctorScheduleRequest request) {
        verifyScheduleWorkingHours(request.startTime(), request.endTime());
        DoctorSchedule savedSchedule = saveOrUpdateDoctorSchedule(request);
        return doctorScheduleMapper.toResponse(savedSchedule);
    }

    @Override
    public DoctorScheduleResponse getDoctorScheduleByDoctorId(String doctorId) {
        return doctorScheduleMapper.toResponse(findExistingDoctorSchedule(doctorId));
    }

    @Override
    public List<DoctorScheduleResponse> getAllDoctorSchedules() {
        return doctorScheduleMapper.toResponseList(doctorScheduleRepository.findAll());
    }

    @Override
    public List<TimeSlotResponse> generateTimeSlots(GenerateScheduleSlotsRequest request) {
        DoctorSchedule schedule = resolveDoctorScheduleForGeneration(request);
        LocalDate startDate = request.startDate() != null ? request.startDate() : LocalDate.now();
        int daysAhead = request.daysAhead() != null ? request.daysAhead() : DEFAULT_DAYS_AHEAD;
        List<TimeSlot> generatedSlots = buildNewTimeSlotsForPeriod(schedule, startDate, daysAhead);
        List<TimeSlot> savedSlots = generatedSlots.isEmpty() ? List.of() : timeSlotRepository.saveAll(generatedSlots);
        return timeSlotMapper.toResponseList(savedSlots);
    }

    private DoctorSchedule resolveDoctorScheduleForGeneration(GenerateScheduleSlotsRequest request) {
        if (hasInlineScheduleTemplate(request)) {
            int duration = request.slotDurationMinutes() != null ? request.slotDurationMinutes() : DEFAULT_SLOT_DURATION_MINUTES;
            verifyScheduleWorkingHours(request.startTime(), request.endTime());
            return saveOrUpdateDoctorSchedule(UpsertDoctorScheduleRequest.builder()
                    .doctorId(request.doctorId())
                    .daysOfWeek(request.daysOfWeek())
                    .startTime(request.startTime())
                    .endTime(request.endTime())
                    .slotDurationMinutes(duration)
                    .roomNumber(request.roomNumber())
                    .active(true)
                    .build());
        }
        return findExistingDoctorSchedule(request.doctorId());
    }

    private boolean hasInlineScheduleTemplate(GenerateScheduleSlotsRequest request) {
        return request.daysOfWeek() != null
                && !request.daysOfWeek().isEmpty()
                && request.startTime() != null
                && request.endTime() != null;
    }

    private DoctorSchedule saveOrUpdateDoctorSchedule(UpsertDoctorScheduleRequest request) {
        DoctorSchedule schedule = doctorScheduleRepository.findByDoctorId(request.doctorId())
                .map(existing -> applyUpdatesToSchedule(existing, request))
                .orElseGet(() -> doctorScheduleMapper.toNewEntity(request));
        return doctorScheduleRepository.save(schedule);
    }

    private DoctorSchedule applyUpdatesToSchedule(DoctorSchedule existing, UpsertDoctorScheduleRequest request) {
        doctorScheduleMapper.updateEntity(existing, request);
        return existing;
    }

    private List<TimeSlot> buildNewTimeSlotsForPeriod(DoctorSchedule schedule, LocalDate startDate, int daysAhead) {
        List<TimeSlot> slotsToCreate = new ArrayList<>();
        for (int dayOffset = 0; dayOffset < daysAhead; dayOffset++) {
            LocalDate currentDate = startDate.plusDays(dayOffset);
            if (schedule.getDaysOfWeek().contains(currentDate.getDayOfWeek())) {
                slotsToCreate.addAll(buildDailySlotsIfMissing(schedule, currentDate));
            }
        }
        return slotsToCreate;
    }

    private List<TimeSlot> buildDailySlotsIfMissing(DoctorSchedule schedule, LocalDate date) {
        List<TimeSlot> dailySlots = new ArrayList<>();
        LocalTime cursor = schedule.getStartTime();
        int stepMinutes = schedule.getSlotDurationMinutes();
        while (!cursor.plusMinutes(stepMinutes).isAfter(schedule.getEndTime())) {
            LocalDateTime slotStart = LocalDateTime.of(date, cursor);
            LocalDateTime slotEnd = slotStart.plusMinutes(stepMinutes);
            if (timeSlotRepository.findByDoctorIdAndStartTime(schedule.getDoctorId(), slotStart).isEmpty()) {
                dailySlots.add(buildAvailableTimeSlot(schedule, slotStart, slotEnd));
            }
            cursor = cursor.plusMinutes(stepMinutes);
        }
        return dailySlots;
    }

    private TimeSlot buildAvailableTimeSlot(DoctorSchedule schedule, LocalDateTime slotStart, LocalDateTime slotEnd) {
        LocalDateTime now = LocalDateTime.now();
        return TimeSlot.builder()
                .doctorId(schedule.getDoctorId())
                .scheduleId(schedule.getId())
                .startTime(slotStart)
                .endTime(slotEnd)
                .roomNumber(schedule.getRoomNumber())
                .status(TimeSlotStatus.AVAILABLE)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private void verifyScheduleWorkingHours(LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new ApiProblemException(HttpStatus.BAD_REQUEST, "invalid-schedule-hours",
                    "Invalid schedule working hours", "End time must be strictly after start time.");
        }
    }

    private DoctorSchedule findExistingDoctorSchedule(String doctorId) {
        return doctorScheduleRepository.findByDoctorId(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Schedule not found for doctorId: " + doctorId));
    }
}
