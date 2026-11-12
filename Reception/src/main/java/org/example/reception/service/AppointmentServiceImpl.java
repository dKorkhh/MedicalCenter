package org.example.reception.service;

import lombok.extern.slf4j.Slf4j;
import org.example.reception.client.BillingServiceAdapter;
import org.example.reception.client.dto.InvoiceDto;
import org.example.reception.dto.AppointmentDetailsResponse;
import org.example.reception.dto.AppointmentResponse;
import org.example.reception.dto.CompleteAppointmentRequest;
import org.example.reception.dto.CreateAppointmentRequest;
import org.example.reception.event.AppointmentBookedEvent;
import org.example.reception.event.AppointmentCancelledEvent;
import org.example.reception.event.AppointmentCompletedEvent;
import org.example.reception.exception.ApiProblemException;
import org.example.reception.mapper.AppointmentMapper;
import org.example.reception.model.Appointment;
import org.example.reception.model.TimeSlot;
import org.example.reception.outbox.AppointmentOutboxService;
import org.example.reception.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AppointmentServiceImpl implements AppointmentService {

    private static final String CANCELLED_STATUS = "CANCELLED";
    private static final String COMPLETED_STATUS = "COMPLETED";
    private static final String DEFAULT_CURRENCY = "UAH";
    private static final String DEFAULT_SERVICE_NAME = "Medical Consultation";

    private final AppointmentRepository appointmentRepository;
    private final BillingServiceAdapter billingServiceAdapter;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentPrerequisitesValidator prerequisitesValidator;
    private final AppointmentOutboxService appointmentOutboxService;
    private final TimeSlotReservationService timeSlotReservationService;

    public AppointmentServiceImpl(
            AppointmentRepository appointmentRepository,
            BillingServiceAdapter billingServiceAdapter,
            AppointmentMapper appointmentMapper,
            AppointmentPrerequisitesValidator prerequisitesValidator,
            AppointmentOutboxService appointmentOutboxService) {
        this(appointmentRepository, billingServiceAdapter, appointmentMapper,
                prerequisitesValidator, appointmentOutboxService, null);
    }

    @Autowired
    public AppointmentServiceImpl(
            AppointmentRepository appointmentRepository,
            BillingServiceAdapter billingServiceAdapter,
            AppointmentMapper appointmentMapper,
            AppointmentPrerequisitesValidator prerequisitesValidator,
            AppointmentOutboxService appointmentOutboxService,
            TimeSlotReservationService timeSlotReservationService) {
        this.appointmentRepository = appointmentRepository;
        this.billingServiceAdapter = billingServiceAdapter;
        this.appointmentMapper = appointmentMapper;
        this.prerequisitesValidator = prerequisitesValidator;
        this.appointmentOutboxService = appointmentOutboxService;
        this.timeSlotReservationService = timeSlotReservationService;
    }

    @Override
    @Transactional
    public AppointmentResponse scheduleAppointment(CreateAppointmentRequest request) {
        prerequisitesValidator.verifyAppointmentPrerequisites(request);
        TimeSlot reservedSlot = reserveTimeSlot(request);

        Appointment appointment = appointmentMapper.toEntity(request);
        applyReservedSlotToAppointment(appointment, reservedSlot);
        Appointment savedAppointment = appointmentRepository.save(appointment);
        bindAppointmentToReservedSlot(reservedSlot, savedAppointment.getId());

        AppointmentBookedEvent bookedEvent = appointmentMapper.toBookedEvent(savedAppointment, request);
        appointmentOutboxService.recordAppointmentBookedEvent(bookedEvent);

        return appointmentMapper.toResponse(savedAppointment);
    }

    @Override
    @Transactional
    public AppointmentResponse cancelAppointment(String id, String cancelledBy, String reason) {
        Appointment appointment = findExistingAppointment(id);
        verifyAppointmentCanBeCancelled(appointment);

        appointment.setAppointmentStatus(CANCELLED_STATUS);
        appointment.setCancelledBy(cancelledBy);
        appointment.setCancellationReason(reason);
        appointment.setCancelledAt(LocalDateTime.now());
        Appointment savedAppointment = appointmentRepository.save(appointment);

        releaseTimeSlotIfConfigured(savedAppointment);
        recordCancellationOutboxEvent(savedAppointment, cancelledBy, reason);

        return appointmentMapper.toResponse(savedAppointment);
    }

    @Override
    @Transactional
    public AppointmentResponse completeAppointment(String id, CompleteAppointmentRequest request) {
        Appointment appointment = findExistingAppointment(id);
        verifyAppointmentCanBeCompleted(appointment);

        appointment.setAppointmentStatus(COMPLETED_STATUS);
        appointment.setCompletedAt(LocalDateTime.now());
        Appointment savedAppointment = appointmentRepository.save(appointment);

        recordCompletionOutboxEvent(savedAppointment, request);

        return appointmentMapper.toResponse(savedAppointment);
    }

    @Override
    public AppointmentResponse getAppointmentById(String id) {
        return appointmentMapper.toResponse(findExistingAppointment(id));
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
    public List<AppointmentResponse> getAppointmentsByDoctorId(String doctorId) {
        return appointmentMapper.toResponseList(appointmentRepository.findByDoctorId(doctorId));
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

    private Appointment findExistingAppointment(String id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found with id: " + id));
    }

    private void verifyAppointmentCanBeCancelled(Appointment appointment) {
        if (CANCELLED_STATUS.equalsIgnoreCase(appointment.getAppointmentStatus())) {
            throw new ApiProblemException(HttpStatus.CONFLICT, "appointment-already-cancelled",
                    "Appointment already cancelled", "Appointment " + appointment.getId() + " is already cancelled.");
        }
        if (COMPLETED_STATUS.equalsIgnoreCase(appointment.getAppointmentStatus())) {
            throw new ApiProblemException(HttpStatus.CONFLICT, "appointment-already-completed",
                    "Appointment already completed", "Completed appointment " + appointment.getId() + " cannot be cancelled.");
        }
    }

    private void verifyAppointmentCanBeCompleted(Appointment appointment) {
        if (COMPLETED_STATUS.equalsIgnoreCase(appointment.getAppointmentStatus())) {
            throw new ApiProblemException(HttpStatus.CONFLICT, "appointment-already-completed",
                    "Appointment already completed", "Appointment " + appointment.getId() + " is already completed.");
        }
        if (CANCELLED_STATUS.equalsIgnoreCase(appointment.getAppointmentStatus())) {
            throw new ApiProblemException(HttpStatus.CONFLICT, "appointment-already-cancelled",
                    "Appointment already cancelled", "Cancelled appointment " + appointment.getId() + " cannot be completed.");
        }
    }

    private TimeSlot reserveTimeSlot(CreateAppointmentRequest request) {
        if (timeSlotReservationService == null) {
            return null;
        }
        return timeSlotReservationService.reserveSlotForAppointment(request);
    }

    private void bindAppointmentToReservedSlot(TimeSlot reservedSlot, String appointmentId) {
        if (timeSlotReservationService != null && reservedSlot != null) {
            timeSlotReservationService.bindAppointmentIdToReservedSlot(reservedSlot, appointmentId);
        }
    }

    private void releaseTimeSlotIfConfigured(Appointment appointment) {
        if (timeSlotReservationService != null) {
            timeSlotReservationService.releaseSlotForCancelledAppointment(appointment);
        }
    }

    private void recordCancellationOutboxEvent(Appointment appointment, String cancelledBy, String reason) {
        InvoiceDto invoice = fetchInvoiceForRefund(appointment.getInvoiceId());
        BigDecimal refundAmount = invoice != null && invoice.amount() != null ? invoice.amount() : BigDecimal.ZERO;
        String currency = invoice != null && invoice.currency() != null ? invoice.currency() : DEFAULT_CURRENCY;

        AppointmentCancelledEvent cancelledEvent = appointmentMapper.toCancelledEvent(
                appointment, refundAmount, currency, cancelledBy, reason);
        appointmentOutboxService.recordAppointmentCancelledEvent(cancelledEvent);
    }

    private void recordCompletionOutboxEvent(Appointment appointment, CompleteAppointmentRequest request) {
        String patientEmail = appointment.getPatientEmail() != null && !appointment.getPatientEmail().isBlank()
                ? appointment.getPatientEmail()
                : "patient-" + appointment.getPatientId() + "@medicalcenter.local";
        String serviceName = request.serviceName() != null && !request.serviceName().isBlank()
                ? request.serviceName()
                : DEFAULT_SERVICE_NAME;
        BigDecimal amount = request.amount() != null ? request.amount() : resolveAppointmentAmount(appointment);
        String currency = request.currency() != null && !request.currency().isBlank() ? request.currency() : DEFAULT_CURRENCY;

        AppointmentCompletedEvent completedEvent = appointmentMapper.toCompletedEvent(
                appointment,
                patientEmail,
                serviceName,
                amount,
                currency,
                request.diagnosis(),
                request.prescription(),
                request.doctorNotes());
        appointmentOutboxService.recordAppointmentCompletedEvent(completedEvent);
    }

    private BigDecimal resolveAppointmentAmount(Appointment appointment) {
        InvoiceDto invoice = fetchInvoiceForRefund(appointment.getInvoiceId());
        return invoice != null && invoice.amount() != null ? invoice.amount() : BigDecimal.ZERO;
    }

    private InvoiceDto fetchInvoiceForRefund(String invoiceId) {
        if (invoiceId == null || invoiceId.isBlank() || billingServiceAdapter == null) {
            return null;
        }
        try {
            return billingServiceAdapter.getInvoice(invoiceId, 0L);
        } catch (Exception ex) {
            log.warn("Unable to resolve invoice {} during appointment processing: {}", invoiceId, ex.getMessage());
            return null;
        }
    }

    private void applyReservedSlotToAppointment(Appointment appointment, TimeSlot slot) {
        if (slot == null) {
            return;
        }
        appointment.setTimeSlotId(slot.getId());
        if (appointment.getDoctorId() == null || appointment.getDoctorId().isBlank()) {
            appointment.setDoctorId(slot.getDoctorId());
        }
        if (appointment.getAppointmentDateTime() == null) {
            appointment.setAppointmentDateTime(slot.getStartTime());
        }
        if ((appointment.getRoomNumber() == null || appointment.getRoomNumber().isBlank()) && slot.getRoomNumber() != null) {
            appointment.setRoomNumber(slot.getRoomNumber());
        }
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
