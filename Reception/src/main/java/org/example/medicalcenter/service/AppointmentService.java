package org.example.medicalcenter.service;

import org.example.medicalcenter.dto.AppointmentResponse;
import org.example.medicalcenter.dto.CreateAppointmentRequest;

import java.util.List;

public interface AppointmentService {

    AppointmentResponse scheduleAppointment(CreateAppointmentRequest request);

    AppointmentResponse getAppointmentById(String id);

    List<AppointmentResponse> getAllAppointments();

    List<AppointmentResponse> getAppointmentsByPatientId(String patientId);
}
