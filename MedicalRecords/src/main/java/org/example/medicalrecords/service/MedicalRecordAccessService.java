package org.example.medicalrecords.service;

import lombok.RequiredArgsConstructor;
import org.example.medicalrecords.dto.CreateMedicalRecordRequest;
import org.example.medicalrecords.dto.MedicalRecordResponse;
import org.example.medicalrecords.security.CurrentUserProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicalRecordAccessService {

    private final MedicalRecordService medicalRecordService;
    private final CurrentUserProvider currentUserProvider;

    public CreateMedicalRecordRequest bindRequestToCurrentDoctor(CreateMedicalRecordRequest request) {
        verifyPatientIdPresent(request);
        if (currentUserProvider.isAdmin()) {
            return request;
        }
        return request.toBuilder().doctorId(currentUserProvider.currentUserId()).build();
    }

    public MedicalRecordResponse getMedicalRecordVisibleToCurrentUser(String id) {
        MedicalRecordResponse medicalRecord = medicalRecordService.getMedicalRecordById(id);
        verifyCurrentUserCanViewMedicalRecord(medicalRecord);
        return medicalRecord;
    }

    public List<MedicalRecordResponse> getMedicalRecordsVisibleToCurrentUser(String requestedPatientId) {
        if (isRestrictedToOwnRecords()) {
            return medicalRecordService.getMedicalRecordsByPatientId(currentUserProvider.currentUserId());
        }
        if (requestedPatientId == null || requestedPatientId.isBlank()) {
            return medicalRecordService.getAllMedicalRecords();
        }
        return medicalRecordService.getMedicalRecordsByPatientId(requestedPatientId);
    }

    private void verifyPatientIdPresent(CreateMedicalRecordRequest request) {
        if (request.patientId() == null || request.patientId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "patientId is required");
        }
    }

    private void verifyCurrentUserCanViewMedicalRecord(MedicalRecordResponse medicalRecord) {
        if (isRestrictedToOwnRecords() && !currentUserProvider.currentUserId().equals(medicalRecord.patientId())) {
            throw new AccessDeniedException("Medical record " + medicalRecord.id() + " does not belong to the current user");
        }
    }

    private boolean isRestrictedToOwnRecords() {
        return !currentUserProvider.isAdmin() && !currentUserProvider.isDoctor();
    }
}
