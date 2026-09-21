package org.example.medicalcenter.service;

import lombok.RequiredArgsConstructor;
import org.example.medicalcenter.dto.AppointmentResponse;
import org.example.medicalcenter.dto.CreateAppointmentRequest;
import org.example.medicalcenter.model.Appointment;
import org.example.medicalcenter.repository.AppointmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;

    @Override
    public AppointmentResponse scheduleAppointment(CreateAppointmentRequest request) {
        Appointment appointment = Appointment.builder()
                .patientId(request.patientId())
                .patientFullName(request.patientFullName())
                .doctorId(request.doctorId())
                .doctorFullName(request.doctorFullName())
                .appointmentDateTime(request.appointmentDateTime())
                .appointmentStatus("SCHEDULED")
                .roomNumber(request.roomNumber())
                .createdAt(LocalDateTime.now())
                .build();

        Appointment savedAppointment = appointmentRepository.save(appointment);
        return mapToResponse(savedAppointment);
    }

    @Override
    public AppointmentResponse getAppointmentById(String id) {
        return appointmentRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found with id: " + id));
    }

    @Override
    public List<AppointmentResponse> getAllAppointments() {
        return appointmentRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<AppointmentResponse> getAppointmentsByPatientId(String patientId) {
        return appointmentRepository.findByPatientId(patientId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private AppointmentResponse mapToResponse(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatientId(),
                appointment.getPatientFullName(),
                appointment.getDoctorId(),
                appointment.getDoctorFullName(),
                appointment.getAppointmentDateTime(),
                appointment.getAppointmentStatus(),
                appointment.getRoomNumber(),
                appointment.getCreatedAt()
        );
    }
}
