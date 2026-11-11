package org.example.reception.service;

import lombok.RequiredArgsConstructor;
import org.example.reception.dto.CreateAppointmentRequest;
import org.example.reception.dto.TimeSlotResponse;
import org.example.reception.exception.ApiProblemException;
import org.example.reception.mapper.TimeSlotMapper;
import org.example.reception.model.Appointment;
import org.example.reception.model.TimeSlot;
import org.example.reception.model.TimeSlotStatus;
import org.example.reception.repository.AppointmentRepository;
import org.example.reception.repository.TimeSlotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TimeSlotReservationServiceImpl implements TimeSlotReservationService {

    private static final String CANCELLED_STATUS = "CANCELLED";

    private final TimeSlotRepository timeSlotRepository;
    private final AppointmentRepository appointmentRepository;
    private final TimeSlotMapper timeSlotMapper;

    @Override
    public List<TimeSlotResponse> getTimeSlots(String doctorId, LocalDate date, TimeSlotStatus status) {
        return timeSlotMapper.toResponseList(queryTimeSlots(doctorId, date, status));
    }

    @Override
    public TimeSlotResponse getTimeSlotById(String slotId) {
        return timeSlotMapper.toResponse(findExistingTimeSlot(slotId));
    }

    @Override
    public TimeSlotResponse holdTimeSlot(String slotId, String patientId) {
        TimeSlot slot = findExistingTimeSlot(slotId);
        verifySlotAvailableForPatient(slot, patientId);
        slot.setStatus(TimeSlotStatus.HELD);
        slot.setHeldByPatientId(patientId);
        slot.setUpdatedAt(LocalDateTime.now());
        return timeSlotMapper.toResponse(timeSlotRepository.save(slot));
    }

    @Override
    public TimeSlotResponse releaseTimeSlot(String slotId, String currentUserId, boolean isAdmin) {
        TimeSlot slot = findExistingTimeSlot(slotId);
        verifySlotCanBeReleased(slot, currentUserId, isAdmin);
        slot.setStatus(TimeSlotStatus.AVAILABLE);
        slot.setHeldByPatientId(null);
        slot.setUpdatedAt(LocalDateTime.now());
        return timeSlotMapper.toResponse(timeSlotRepository.save(slot));
    }

    @Override
    public TimeSlot reserveSlotForAppointment(CreateAppointmentRequest request) {
        verifyTimeSlotIdProvided(request.timeSlotId());
        TimeSlot slot = findExistingTimeSlot(request.timeSlotId());
        verifySlotMatchesAppointmentDoctorAndTime(slot, request);
        verifyDoctorTimeNotAlreadyBooked(slot.getDoctorId(), slot.getStartTime());
        verifySlotAvailableForPatient(slot, request.patientId());
        slot.setStatus(TimeSlotStatus.BOOKED);
        slot.setHeldByPatientId(request.patientId());
        slot.setUpdatedAt(LocalDateTime.now());
        return timeSlotRepository.save(slot);
    }

    @Override
    public void bindAppointmentIdToReservedSlot(TimeSlot slot, String appointmentId) {
        slot.setAppointmentId(appointmentId);
        slot.setUpdatedAt(LocalDateTime.now());
        timeSlotRepository.save(slot);
    }

    @Override
    public void releaseSlotForCancelledAppointment(Appointment appointment) {
        findSlotLinkedToAppointment(appointment).ifPresent(slot -> {
            slot.setStatus(TimeSlotStatus.AVAILABLE);
            slot.setHeldByPatientId(null);
            slot.setAppointmentId(null);
            slot.setUpdatedAt(LocalDateTime.now());
            timeSlotRepository.save(slot);
        });
    }

    private void verifyTimeSlotIdProvided(String timeSlotId) {
        if (timeSlotId == null || timeSlotId.isBlank()) {
            throw new ApiProblemException(HttpStatus.BAD_REQUEST, "timeslot-id-required",
                    "Time slot ID required", "Booking an appointment requires a valid timeSlotId.");
        }
    }

    private Optional<TimeSlot> findSlotLinkedToAppointment(Appointment appointment) {
        if (timeSlotRepository == null) {
            return Optional.empty();
        }
        if (appointment.getTimeSlotId() != null && !appointment.getTimeSlotId().isBlank()) {
            return timeSlotRepository.findById(appointment.getTimeSlotId());
        }
        return timeSlotRepository.findByDoctorIdAndStartTime(
                appointment.getDoctorId(), appointment.getAppointmentDateTime());
    }

    private void verifyDoctorTimeNotAlreadyBooked(String doctorId, LocalDateTime appointmentDateTime) {
        if (appointmentRepository != null
                && appointmentRepository.existsByDoctorIdAndAppointmentDateTimeAndAppointmentStatusNot(
                        doctorId, appointmentDateTime, CANCELLED_STATUS)) {
            throw new ApiProblemException(HttpStatus.CONFLICT, "slot-already-booked",
                    "Time slot already booked", "Doctor already has an active appointment at " + appointmentDateTime);
        }
    }

    private void verifySlotMatchesAppointmentDoctorAndTime(TimeSlot slot, CreateAppointmentRequest request) {
        if (request.doctorId() != null && !request.doctorId().isBlank() && !slot.getDoctorId().equals(request.doctorId())) {
            throw new ApiProblemException(HttpStatus.BAD_REQUEST, "slot-mismatch",
                    "Time slot mismatch", "Provided doctorId does not match the time slot's doctorId.");
        }
        if (request.appointmentDateTime() != null && !slot.getStartTime().equals(request.appointmentDateTime())) {
            throw new ApiProblemException(HttpStatus.BAD_REQUEST, "slot-mismatch",
                    "Time slot mismatch", "Provided appointmentDateTime does not match the time slot's startTime.");
        }
    }

    private void verifySlotAvailableForPatient(TimeSlot slot, String patientId) {
        boolean available = slot.getStatus() == TimeSlotStatus.AVAILABLE
                || (slot.getStatus() == TimeSlotStatus.HELD
                        && patientId != null
                        && patientId.equals(slot.getHeldByPatientId()));
        if (!available) {
            throw new ApiProblemException(HttpStatus.CONFLICT, "slot-unavailable",
                    "Time slot unavailable", "Time slot " + slot.getId() + " is already " + slot.getStatus());
        }
    }

    private void verifySlotCanBeReleased(TimeSlot slot, String currentUserId, boolean isAdmin) {
        if (slot.getStatus() == TimeSlotStatus.BOOKED) {
            throw new ApiProblemException(HttpStatus.CONFLICT, "slot-already-booked",
                    "Time slot is booked", "Booked time slot cannot be released directly; cancel the appointment instead.");
        }
        if (slot.getStatus() == TimeSlotStatus.HELD && !isAdmin && !currentUserId.equals(slot.getHeldByPatientId())) {
            throw new AccessDeniedException("Time slot " + slot.getId() + " is held by another patient");
        }
    }

    private List<TimeSlot> queryTimeSlots(String doctorId, LocalDate date, TimeSlotStatus status) {
        if (date != null) {
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            if (status != null) {
                return timeSlotRepository.findByDoctorIdAndStatusAndStartTimeBetweenOrderByStartTimeAsc(
                        doctorId, status, startOfDay, endOfDay);
            }
            return timeSlotRepository.findByDoctorIdAndStartTimeBetweenOrderByStartTimeAsc(doctorId, startOfDay, endOfDay);
        }
        if (status != null) {
            return timeSlotRepository.findByDoctorIdAndStatusOrderByStartTimeAsc(doctorId, status);
        }
        return timeSlotRepository.findByDoctorIdOrderByStartTimeAsc(doctorId);
    }

    private TimeSlot findExistingTimeSlot(String slotId) {
        return timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Time slot not found with id: " + slotId));
    }
}
