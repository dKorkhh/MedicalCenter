package org.example.reception.service;

import lombok.RequiredArgsConstructor;
import org.example.reception.dto.PatientProfileResponse;
import org.example.reception.dto.UpsertPatientProfileRequest;
import org.example.reception.security.CurrentUserProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientProfileAccessService {

    private final PatientProfileService patientProfileService;
    private final CurrentUserProvider currentUserProvider;

    public PatientProfileResponse getCurrentPatientProfile() {
        return patientProfileService.getPatientProfileById(currentUserProvider.currentUserId());
    }

    public PatientProfileResponse upsertCurrentPatientProfile(UpsertPatientProfileRequest request) {
        return patientProfileService.upsertPatientProfileById(currentUserProvider.currentUserId(), request);
    }

    public PatientProfileResponse updatePatientProfileVisibleToCurrentUser(String id, UpsertPatientProfileRequest request) {
        verifyCurrentUserCanModifyPatientProfile(id);
        return patientProfileService.updatePatientProfile(id, request);
    }

    public PatientProfileResponse getPatientProfileVisibleToCurrentUser(String id) {
        verifyCurrentUserCanViewPatientProfile(id);
        return patientProfileService.getPatientProfileById(id);
    }

    public List<PatientProfileResponse> getAllPatientProfilesVisibleToCurrentUser() {
        verifyCurrentUserIsDoctorOrAdmin();
        return patientProfileService.getAllPatientProfiles();
    }

    private void verifyCurrentUserCanModifyPatientProfile(String targetPatientId) {
        if (!currentUserProvider.isAdmin() && !currentUserProvider.currentUserId().equals(targetPatientId)) {
            throw new AccessDeniedException("Patient profile " + targetPatientId + " does not belong to the current user");
        }
    }

    private void verifyCurrentUserCanViewPatientProfile(String targetPatientId) {
        boolean allowed = currentUserProvider.isAdmin()
                || currentUserProvider.isDoctor()
                || currentUserProvider.currentUserId().equals(targetPatientId);
        if (!allowed) {
            throw new AccessDeniedException("Patient profile " + targetPatientId + " does not belong to the current user");
        }
    }

    private void verifyCurrentUserIsDoctorOrAdmin() {
        if (!currentUserProvider.isAdmin() && !currentUserProvider.isDoctor()) {
            throw new AccessDeniedException("Listing all patient profiles requires DOCTOR or ADMIN role");
        }
    }
}
