package org.example.reception.service;

import org.example.reception.dto.PatientProfileResponse;
import org.example.reception.dto.UpsertPatientProfileRequest;

import java.util.List;

public interface PatientProfileService {

    PatientProfileResponse createPatientProfile(UpsertPatientProfileRequest request);

    PatientProfileResponse upsertPatientProfileById(String id, UpsertPatientProfileRequest request);

    PatientProfileResponse updatePatientProfile(String id, UpsertPatientProfileRequest request);

    PatientProfileResponse getPatientProfileById(String id);

    List<PatientProfileResponse> getAllPatientProfiles();

    void deletePatientProfileById(String id);
}
