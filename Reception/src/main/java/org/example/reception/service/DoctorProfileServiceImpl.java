package org.example.reception.service;

import org.example.reception.dto.CreateDoctorProfileRequest;
import org.example.reception.dto.DoctorProfileResponse;
import org.example.reception.dto.UpdateDoctorProfileRequest;
import org.example.reception.mapper.DoctorProfileMapper;
import org.example.reception.model.DoctorProfile;
import org.example.reception.repository.DoctorProfileRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DoctorProfileServiceImpl
        extends AbstractSpecializableCatalogService<DoctorProfile, CreateDoctorProfileRequest, UpdateDoctorProfileRequest, DoctorProfileResponse>
        implements DoctorProfileService {

    public DoctorProfileServiceImpl(DoctorProfileRepository doctorProfileRepository, DoctorProfileMapper doctorProfileMapper) {
        super(doctorProfileRepository, doctorProfileMapper, "Doctor profile");
    }

    @Override
    public DoctorProfileResponse createDoctorProfile(CreateDoctorProfileRequest request) {
        return createCatalogItem(request);
    }

    @Override
    public DoctorProfileResponse updateDoctorProfile(String id, UpdateDoctorProfileRequest request) {
        return updateCatalogItem(id, request);
    }

    @Override
    public DoctorProfileResponse getDoctorProfileById(String id) {
        return getCatalogItemById(id);
    }

    @Override
    public List<DoctorProfileResponse> getDoctorProfiles(String specialization, Boolean activeOnly) {
        return getCatalogItems(specialization, activeOnly);
    }

    @Override
    public void deleteDoctorProfileById(String id) {
        deleteCatalogItemById(id);
    }
}
