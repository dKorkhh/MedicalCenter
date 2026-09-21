package org.example.medicalrecords.service;

import org.example.medicalrecords.dto.CreateMedicalRecordRequest;
import org.example.medicalrecords.dto.MedicalRecordResponse;

import java.util.List;

public interface MedicalRecordService {

    MedicalRecordResponse createMedicalRecord(CreateMedicalRecordRequest request);

    MedicalRecordResponse getMedicalRecordById(String id);

    List<MedicalRecordResponse> getAllMedicalRecords();

    List<MedicalRecordResponse> getMedicalRecordsByPatientId(String patientId);
}
