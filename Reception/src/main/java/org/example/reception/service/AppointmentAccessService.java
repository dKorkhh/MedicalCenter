package org.example.reception.service;

import lombok.RequiredArgsConstructor;
import org.example.reception.dto.AppointmentDetailsResponse;
import org.example.reception.dto.AppointmentResponse;
import org.example.reception.dto.CancelAppointmentRequest;
import org.example.reception.dto.CompleteAppointmentRequest;
import org.example.reception.dto.CreateAppointmentRequest;
import org.example.reception.security.CurrentUserProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentAccessService {

    private final AppointmentService appointmentService;
    private final CurrentUserProvider currentUserProvider;

    public CreateAppointmentRequest bindRequestToCurrentPatient(CreateAppointmentRequest request) {
        if (currentUserProvider.isAdmin()) {
            return requirePatientIdProvidedByAdmin(request);
        }
        return request.toBuilder().patientId(currentUserProvider.currentUserId()).build();
    }

    public AppointmentResponse getAppointmentVisibleToCurrentUser(String id) {
        AppointmentResponse appointment = appointmentService.getAppointmentById(id);
        verifyCurrentUserCanViewAppointment(appointment);
        return appointment;
    }

    public AppointmentResponse cancelAppointmentVisibleToCurrentUser(String id, CancelAppointmentRequest request) {
        AppointmentResponse appointment = appointmentService.getAppointmentById(id);
        verifyCurrentUserCanViewAppointment(appointment);
        String reason = request != null ? request.reason() : null;
        return appointmentService.cancelAppointment(id, resolveCancelledByRole(), reason);
    }

    public AppointmentResponse completeAppointmentVisibleToDoctorOrAdmin(String id, CompleteAppointmentRequest request) {
        AppointmentResponse appointment = appointmentService.getAppointmentById(id);
        verifyCurrentUserCanCompleteAppointment(appointment);
        return appointmentService.completeAppointment(id, request);
    }

    public List<AppointmentResponse> getAppointmentsVisibleToCurrentUser(String requestedPatientId) {
        if (currentUserProvider.isAdmin()) {
            return getAppointmentsForAdmin(requestedPatientId);
        }
        if (currentUserProvider.isDoctor()) {
            return appointmentService.getAppointmentsByDoctorId(currentUserProvider.currentUserId());
        }
        return appointmentService.getAppointmentsByPatientId(currentUserProvider.currentUserId());
    }

    public List<AppointmentDetailsResponse> getAppointmentDetailsVisibleToCurrentUser(String requestedPatientId) {
        return appointmentService.getAppointmentDetails(resolvePatientIdForCurrentUser(requestedPatientId));
    }

    private String resolveCancelledByRole() {
        if (currentUserProvider.isAdmin()) {
            return "ADMIN";
        }
        if (currentUserProvider.isDoctor()) {
            return "DOCTOR";
        }
        return "PATIENT";
    }

    private List<AppointmentResponse> getAppointmentsForAdmin(String requestedPatientId) {
        if (requestedPatientId == null || requestedPatientId.isBlank()) {
            return appointmentService.getAllAppointments();
        }
        return appointmentService.getAppointmentsByPatientId(requestedPatientId);
    }

    private String resolvePatientIdForCurrentUser(String requestedPatientId) {
        return currentUserProvider.isAdmin() ? requestedPatientId : currentUserProvider.currentUserId();
    }

    private CreateAppointmentRequest requirePatientIdProvidedByAdmin(CreateAppointmentRequest request) {
        if (request.patientId() == null || request.patientId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patientId is required when an administrator books an appointment");
        }
        return request;
    }

    private void verifyCurrentUserCanViewAppointment(AppointmentResponse appointment) {
        if (!isAppointmentVisibleToCurrentUser(appointment)) {
            throw new AccessDeniedException("Appointment " + appointment.id() + " does not belong to the current user");
        }
    }

    private void verifyCurrentUserCanCompleteAppointment(AppointmentResponse appointment) {
        String currentUserId = currentUserProvider.currentUserId();
        boolean canComplete = currentUserProvider.isAdmin()
                || (currentUserProvider.isDoctor() && currentUserId.equals(appointment.doctorId()));
        if (!canComplete) {
            throw new AccessDeniedException("Only the assigned doctor or an administrator can complete appointment " + appointment.id());
        }
    }

    private boolean isAppointmentVisibleToCurrentUser(AppointmentResponse appointment) {
        String currentUserId = currentUserProvider.currentUserId();
        return currentUserProvider.isAdmin()
                || (currentUserProvider.isDoctor() && currentUserId.equals(appointment.doctorId()))
                || (currentUserProvider.isPatient() && currentUserId.equals(appointment.patientId()));
    }
}
