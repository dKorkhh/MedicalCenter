package org.example.reception.service;

import org.example.reception.dto.AppointmentDetailsResponse;
import org.example.reception.dto.AppointmentResponse;
import org.example.reception.dto.CreateAppointmentRequest;

import java.util.List;

public interface AppointmentService {

    AppointmentResponse scheduleAppointment(CreateAppointmentRequest request);

    AppointmentResponse cancelAppointment(String id, String cancelledBy, String reason);

    AppointmentResponse getAppointmentById(String id);

    List<AppointmentResponse> getAllAppointments();

    List<AppointmentResponse> getAppointmentsByPatientId(String patientId);

    List<AppointmentResponse> getAppointmentsByDoctorId(String doctorId);

    List<AppointmentDetailsResponse> getAppointmentDetails(String patientId);
}
