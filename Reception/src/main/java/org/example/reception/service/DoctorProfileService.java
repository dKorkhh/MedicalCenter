package org.example.reception.service;

import org.example.reception.dto.CreateDoctorProfileRequest;
import org.example.reception.dto.DoctorProfileResponse;
import org.example.reception.dto.UpdateDoctorProfileRequest;

import java.util.List;

public interface DoctorProfileService {

    DoctorProfileResponse createDoctorProfile(CreateDoctorProfileRequest request);

    DoctorProfileResponse updateDoctorProfile(String id, UpdateDoctorProfileRequest request);

    DoctorProfileResponse getDoctorProfileById(String id);

    List<DoctorProfileResponse> getDoctorProfiles(String specialization, Boolean activeOnly);

    void deleteDoctorProfileById(String id);
}
