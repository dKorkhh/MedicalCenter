package org.example.reception.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.reception.client.BillingServiceAdapter;
import org.example.reception.client.dto.InvoiceDto;
import org.example.reception.dto.AppointmentDetailsResponse;
import org.example.reception.dto.AppointmentResponse;
import org.example.reception.dto.CreateAppointmentRequest;
import org.example.reception.event.AppointmentBookedEvent;
import org.example.reception.mapper.AppointmentMapper;
import org.example.reception.model.Appointment;
import org.example.reception.outbox.AppointmentOutboxService;
import org.example.reception.repository.AppointmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final BillingServiceAdapter billingServiceAdapter;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentPrerequisitesValidator prerequisitesValidator;
    private final AppointmentOutboxService appointmentOutboxService;

    @Override
    @Transactional
    public AppointmentResponse scheduleAppointment(CreateAppointmentRequest request) {
        prerequisitesValidator.verifyAppointmentPrerequisites(request);

        Appointment appointment = appointmentMapper.toEntity(request);
        Appointment savedAppointment = appointmentRepository.save(appointment);

        AppointmentBookedEvent bookedEvent = appointmentMapper.toBookedEvent(savedAppointment, request);
        appointmentOutboxService.recordAppointmentBookedEvent(bookedEvent);

        return appointmentMapper.toResponse(savedAppointment);
    }

    @Override
    public AppointmentResponse getAppointmentById(String id) {
        return appointmentRepository.findById(id)
                .map(appointmentMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found with id: " + id));
    }

    @Override
    public List<AppointmentResponse> getAllAppointments() {
        return appointmentMapper.toResponseList(appointmentRepository.findAll());
    }

    @Override
    public List<AppointmentResponse> getAppointmentsByPatientId(String patientId) {
        return appointmentMapper.toResponseList(appointmentRepository.findByPatientId(patientId));
    }

    @Override
    public List<AppointmentDetailsResponse> getAppointmentDetails(String patientId) {
        List<AppointmentResponse> appointments = (patientId != null && !patientId.isBlank())
                ? getAppointmentsByPatientId(patientId)
                : getAllAppointments();

        Map<String, InvoiceDto> invoicesById = fetchInvoicesByAppointments(appointments);

        return appointments.stream()
                .map(appointment -> new AppointmentDetailsResponse(
                        appointment,
                        appointment.invoiceId() == null ? null : invoicesById.get(appointment.invoiceId())))
                .toList();
    }

    private Map<String, InvoiceDto> fetchInvoicesByAppointments(List<AppointmentResponse> appointments) {
        List<String> invoiceIds = appointments.stream()
                .map(AppointmentResponse::invoiceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return billingServiceAdapter.getInvoicesBatch(invoiceIds).stream()
                .filter(invoice -> invoice.id() != null)
                .collect(Collectors.toMap(InvoiceDto::id, Function.identity(), (first, second) -> first));
    }
}
