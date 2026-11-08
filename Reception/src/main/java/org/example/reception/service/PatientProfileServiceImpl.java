package org.example.reception.service;

import lombok.RequiredArgsConstructor;
import org.example.reception.dto.PatientProfileResponse;
import org.example.reception.dto.UpsertPatientProfileRequest;
import org.example.reception.mapper.PatientProfileMapper;
import org.example.reception.model.PatientProfile;
import org.example.reception.repository.PatientProfileRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientProfileServiceImpl implements PatientProfileService {

    private final PatientProfileRepository patientProfileRepository;
    private final PatientProfileMapper patientProfileMapper;

    @Override
    public PatientProfileResponse createPatientProfile(UpsertPatientProfileRequest request) {
        PatientProfile entity = patientProfileMapper.toNewEntity(null, request);
        PatientProfile savedEntity = patientProfileRepository.save(entity);
        return patientProfileMapper.toResponse(savedEntity);
    }

    @Override
    public PatientProfileResponse upsertPatientProfileById(String id, UpsertPatientProfileRequest request) {
        PatientProfile entity = patientProfileRepository.findById(id)
                .map(existing -> applyUpdatesToPatientProfile(existing, request))
                .orElseGet(() -> patientProfileMapper.toNewEntity(id, request));
        PatientProfile savedEntity = patientProfileRepository.save(entity);
        return patientProfileMapper.toResponse(savedEntity);
    }

    @Override
    public PatientProfileResponse updatePatientProfile(String id, UpsertPatientProfileRequest request) {
        PatientProfile existingEntity = findExistingPatientProfile(id);
        patientProfileMapper.updateEntity(existingEntity, request);
        PatientProfile savedEntity = patientProfileRepository.save(existingEntity);
        return patientProfileMapper.toResponse(savedEntity);
    }

    @Override
    public PatientProfileResponse getPatientProfileById(String id) {
        return patientProfileMapper.toResponse(findExistingPatientProfile(id));
    }

    @Override
    public List<PatientProfileResponse> getAllPatientProfiles() {
        return patientProfileMapper.toResponseList(patientProfileRepository.findAll());
    }

    @Override
    public void deletePatientProfileById(String id) {
        PatientProfile existingEntity = findExistingPatientProfile(id);
        patientProfileRepository.delete(existingEntity);
    }

    private PatientProfile applyUpdatesToPatientProfile(PatientProfile entity, UpsertPatientProfileRequest request) {
        patientProfileMapper.updateEntity(entity, request);
        return entity;
    }

    private PatientProfile findExistingPatientProfile(String id) {
        return patientProfileRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient profile not found with id: " + id));
    }
}
