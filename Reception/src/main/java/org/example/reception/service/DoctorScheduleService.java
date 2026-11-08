package org.example.reception.service;

import org.example.reception.dto.DoctorScheduleResponse;
import org.example.reception.dto.GenerateScheduleSlotsRequest;
import org.example.reception.dto.TimeSlotResponse;
import org.example.reception.dto.UpsertDoctorScheduleRequest;

import java.util.List;

public interface DoctorScheduleService {

    DoctorScheduleResponse upsertDoctorSchedule(UpsertDoctorScheduleRequest request);

    DoctorScheduleResponse getDoctorScheduleByDoctorId(String doctorId);

    List<DoctorScheduleResponse> getAllDoctorSchedules();

    List<TimeSlotResponse> generateTimeSlots(GenerateScheduleSlotsRequest request);
}
