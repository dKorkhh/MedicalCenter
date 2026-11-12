package org.example.billing.service;

import lombok.RequiredArgsConstructor;
import org.example.billing.dto.FinancialActResponse;
import org.example.billing.dto.PaymentSnapshotResponse;
import org.example.billing.repository.FinancialActRepository;
import org.example.billing.security.CurrentUserProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancialActAccessService {

    private final FinancialActService financialActService;
    private final FinancialActRepository financialActRepository;
    private final CurrentUserProvider currentUserProvider;

    public FinancialActResponse getFinancialActVisibleToCurrentUser(String id) {
        FinancialActResponse act = financialActService.getFinancialActById(id);
        verifyActAccess(act);
        return act;
    }

    public FinancialActResponse getFinancialActByAppointmentIdVisibleToCurrentUser(String appointmentId) {
        FinancialActResponse act = financialActService.getFinancialActByAppointmentId(appointmentId);
        verifyActAccess(act);
        return act;
    }

    public List<FinancialActResponse> getFinancialActsVisibleToCurrentUser(String requestedPatientId, String requestedDoctorId) {
        if (currentUserProvider.isAdmin()) {
            if (requestedPatientId != null && !requestedPatientId.isBlank()) {
                return financialActService.getFinancialActsByPatientId(requestedPatientId);
            }
            if (requestedDoctorId != null && !requestedDoctorId.isBlank()) {
                return financialActService.getFinancialActsByDoctorId(requestedDoctorId);
            }
            return financialActService.getAllFinancialActs();
        }

        if (currentUserProvider.isDoctor()) {
            return financialActService.getFinancialActsByDoctorId(currentUserProvider.currentUserId());
        }

        return financialActService.getFinancialActsByPatientId(currentUserProvider.currentUserId());
    }

    public PaymentSnapshotResponse getPaymentSnapshotVisibleToCurrentUser(String appointmentId) {
        PaymentSnapshotResponse snapshot = financialActService.getPaymentSnapshotByAppointmentId(appointmentId);
        if (currentUserProvider.isAdmin()) {
            return snapshot;
        }

        String currentUserId = currentUserProvider.currentUserId();
        if (currentUserId.equals(snapshot.patientId())) {
            return snapshot;
        }

        if (currentUserProvider.isDoctor()) {
            boolean isDoctorOfAppointment = financialActRepository.findByAppointmentId(appointmentId)
                    .map(act -> currentUserId.equals(act.getDoctorId()))
                    .orElse(false);
            if (isDoctorOfAppointment) {
                return snapshot;
            }
        }

        throw new AccessDeniedException("Access denied to payment snapshot for appointment " + appointmentId);
    }

    private void verifyActAccess(FinancialActResponse act) {
        if (currentUserProvider.isAdmin()) {
            return;
        }
        String currentUserId = currentUserProvider.currentUserId();
        if (currentUserProvider.isDoctor() && currentUserId.equals(act.doctorId())) {
            return;
        }
        if (currentUserId.equals(act.patientId())) {
            return;
        }
        throw new AccessDeniedException("Financial act " + act.id() + " does not belong to the current user");
    }
}
