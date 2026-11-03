package org.example.reception.mapper;

import org.example.reception.dto.AppointmentResponse;
import org.example.reception.dto.CreateAppointmentRequest;
import org.example.reception.event.AppointmentBookedEvent;
import org.example.reception.model.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, imports = {LocalDateTime.class, UUID.class})
public interface AppointmentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "appointmentStatus", constant = "SCHEDULED")
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    Appointment toEntity(CreateAppointmentRequest request);

    AppointmentResponse toResponse(Appointment appointment);

    List<AppointmentResponse> toResponseList(List<Appointment> appointments);

    @Mapping(target = "eventId", expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "eventType", constant = "APPOINTMENT_BOOKED")
    @Mapping(target = "appointmentId", source = "appointment.id")
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

    default String resolvePatientEmail(CreateAppointmentRequest request, Appointment appointment) {
        if (request != null && request.patientEmail() != null && !request.patientEmail().isBlank()) {
            return request.patientEmail();
        }
        return "patient-" + (appointment != null ? appointment.getPatientId() : "") + "@medicalcenter.local";
    }
}
