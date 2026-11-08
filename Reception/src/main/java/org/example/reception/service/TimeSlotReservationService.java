package org.example.reception.service;

import org.example.reception.dto.CreateAppointmentRequest;
import org.example.reception.dto.TimeSlotResponse;
import org.example.reception.model.TimeSlot;
import org.example.reception.model.TimeSlotStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TimeSlotReservationService {

    List<TimeSlotResponse> getTimeSlots(String doctorId, LocalDate date, TimeSlotStatus status);

    TimeSlotResponse getTimeSlotById(String slotId);

    TimeSlotResponse holdTimeSlot(String slotId, String patientId);

    TimeSlotResponse releaseTimeSlot(String slotId, String currentUserId, boolean isAdmin);

    Optional<TimeSlot> reserveSlotForAppointment(CreateAppointmentRequest request);

    void bindAppointmentIdToReservedSlot(TimeSlot slot, String appointmentId);
}
