package org.example.reception.service;

import lombok.RequiredArgsConstructor;
import org.example.reception.client.BillingServiceAdapter;
import org.example.reception.client.dto.InvoiceDto;
import org.example.reception.dto.AppointmentDetailsResponse;
import org.example.reception.dto.AppointmentResponse;
import org.example.reception.dto.CreateAppointmentRequest;
import org.example.reception.model.Appointment;
import org.example.reception.repository.AppointmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final BillingServiceAdapter billingServiceAdapter;

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
                .invoiceId(request.invoiceId())
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

    @Override
    public List<AppointmentDetailsResponse> getAppointmentDetails(String patientId) {
        List<AppointmentResponse> appointments = (patientId != null && !patientId.isBlank())
                ? getAppointmentsByPatientId(patientId)
                : getAllAppointments();

        List<String> invoiceIds = appointments.stream()
                .map(AppointmentResponse::invoiceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, InvoiceDto> invoicesById = billingServiceAdapter.getInvoicesBatch(invoiceIds).stream()
                .filter(invoice -> invoice.id() != null)
                .collect(Collectors.toMap(InvoiceDto::id, Function.identity(), (first, second) -> first));

        return appointments.stream()
                .map(a -> new AppointmentDetailsResponse(a, a.invoiceId() == null ? null : invoicesById.get(a.invoiceId())))
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
                appointment.getInvoiceId(),
                appointment.getCreatedAt()
        );
    }
}
