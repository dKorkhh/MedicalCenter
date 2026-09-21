package org.example.medicalrecords.service;

import lombok.RequiredArgsConstructor;
import org.example.medicalrecords.dto.CreateMedicalRecordRequest;
import org.example.medicalrecords.dto.MedicalRecordResponse;
import org.example.medicalrecords.model.MedicalRecord;
import org.example.medicalrecords.repository.MedicalRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;

    @Override
    public MedicalRecordResponse createMedicalRecord(CreateMedicalRecordRequest request) {
        MedicalRecord medicalRecord = MedicalRecord.builder()
                .patientId(request.patientId())
                .doctorId(request.doctorId())
                .appointmentId(request.appointmentId())
                .diagnosis(request.diagnosis())
                .prescription(request.prescription())
                .doctorNotes(request.doctorNotes())
                .recordDate(LocalDateTime.now())
                .build();

        MedicalRecord savedRecord = medicalRecordRepository.save(medicalRecord);
        return mapToResponse(savedRecord);
    }

    @Override
    public MedicalRecordResponse getMedicalRecordById(String id) {
        return medicalRecordRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medical record not found with id: " + id));
    }

    @Override
    public List<MedicalRecordResponse> getAllMedicalRecords() {
        return medicalRecordRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<MedicalRecordResponse> getMedicalRecordsByPatientId(String patientId) {
        return medicalRecordRepository.findByPatientId(patientId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private MedicalRecordResponse mapToResponse(MedicalRecord record) {
        return new MedicalRecordResponse(
                record.getId(),
                record.getPatientId(),
                record.getDoctorId(),
                record.getAppointmentId(),
                record.getDiagnosis(),
                record.getPrescription(),
                record.getDoctorNotes(),
                record.getRecordDate()
        );
    }
}
