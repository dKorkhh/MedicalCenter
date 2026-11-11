package org.example.reception.mapper;

import org.example.reception.dto.AppointmentResponse;
import org.example.reception.dto.CreateAppointmentRequest;
import org.example.reception.event.AppointmentBookedEvent;
import org.example.reception.event.AppointmentCancelledEvent;
import org.example.reception.event.AppointmentCompletedEvent;
import org.example.reception.model.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, imports = {LocalDateTime.class, UUID.class})
public interface AppointmentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "appointmentStatus", constant = "SCHEDULED")
    @Mapping(target = "cancelledBy", ignore = true)
    @Mapping(target = "cancellationReason", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    Appointment toEntity(CreateAppointmentRequest request);

    AppointmentResponse toResponse(Appointment appointment);

    List<AppointmentResponse> toResponseList(List<Appointment> appointments);

    @Mapping(target = "eventId", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "eventType", constant = "APPOINTMENT_BOOKED")
    @Mapping(target = "appointmentId", source = "appointment.id")
    @Mapping(target = "timeSlotId", source = "appointment.timeSlotId")
    @Mapping(target = "patientId", source = "appointment.patientId")
    @Mapping(target = "patientFullName", source = "appointment.patientFullName")
    @Mapping(target = "patientEmail", expression = "java(resolvePatientEmail(request, appointment))")
    @Mapping(target = "doctorId", source = "appointment.doctorId")
    @Mapping(target = "doctorFullName", source = "appointment.doctorFullName")
    @Mapping(target = "appointmentDateTime", source = "appointment.appointmentDateTime")
    @Mapping(target = "roomNumber", source = "appointment.roomNumber")
    @Mapping(target = "invoiceId", source = "appointment.invoiceId")
    @Mapping(target = "timestamp", expression = "java(LocalDateTime.now())")
    AppointmentBookedEvent toBookedEvent(Appointment appointment, CreateAppointmentRequest request);

    @Mapping(target = "eventId", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "eventType", constant = "APPOINTMENT_CANCELLED")
    @Mapping(target = "appointmentId", source = "appointment.id")
    @Mapping(target = "timeSlotId", source = "appointment.timeSlotId")
    @Mapping(target = "patientId", source = "appointment.patientId")
    @Mapping(target = "patientFullName", source = "appointment.patientFullName")
    @Mapping(target = "patientEmail", expression = "java(resolvePatientEmailFromAppointment(appointment))")
    @Mapping(target = "doctorId", source = "appointment.doctorId")
    @Mapping(target = "doctorFullName", source = "appointment.doctorFullName")
    @Mapping(target = "appointmentDateTime", source = "appointment.appointmentDateTime")
    @Mapping(target = "invoiceId", source = "appointment.invoiceId")
    @Mapping(target = "refundAmount", source = "refundAmount")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "cancelledBy", source = "cancelledBy")
    @Mapping(target = "cancellationReason", source = "cancellationReason")
    @Mapping(target = "timestamp", expression = "java(LocalDateTime.now())")
    AppointmentCancelledEvent toCancelledEvent(
            Appointment appointment,
            BigDecimal refundAmount,
            String currency,
            String cancelledBy,
            String cancellationReason);

    @Mapping(target = "eventId", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "eventType", constant = "APPOINTMENT_COMPLETED")
    @Mapping(target = "appointmentId", source = "appointment.id")
    @Mapping(target = "timeSlotId", source = "appointment.timeSlotId")
    @Mapping(target = "patientId", source = "appointment.patientId")
    @Mapping(target = "patientFullName", source = "appointment.patientFullName")
    @Mapping(target = "patientEmail", source = "patientEmail")
    @Mapping(target = "doctorId", source = "appointment.doctorId")
    @Mapping(target = "doctorFullName", source = "appointment.doctorFullName")
    @Mapping(target = "appointmentDateTime", source = "appointment.appointmentDateTime")
    @Mapping(target = "roomNumber", source = "appointment.roomNumber")
    @Mapping(target = "serviceName", source = "serviceName")
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "currency", source = "currency")
    @Mapping(target = "invoiceId", source = "appointment.invoiceId")
    @Mapping(target = "diagnosis", source = "diagnosis")
    @Mapping(target = "prescription", source = "prescription")
    @Mapping(target = "doctorNotes", source = "doctorNotes")
    @Mapping(target = "completedAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "timestamp", expression = "java(LocalDateTime.now())")
    AppointmentCompletedEvent toCompletedEvent(
            Appointment appointment,
            String patientEmail,
            String serviceName,
            BigDecimal amount,
            String currency,
            String diagnosis,
            String prescription,
            String doctorNotes);

    default String resolvePatientEmail(CreateAppointmentRequest request, Appointment appointment) {
        if (request != null && request.patientEmail() != null && !request.patientEmail().isBlank()) {
            return request.patientEmail();
        }
        return resolvePatientEmailFromAppointment(appointment);
    }

    default String resolvePatientEmailFromAppointment(Appointment appointment) {
        if (appointment != null && appointment.getPatientEmail() != null && !appointment.getPatientEmail().isBlank()) {
            return appointment.getPatientEmail();
        }
        return "patient-" + (appointment != null ? appointment.getPatientId() : "") + "@medicalcenter.local";
    }
}
