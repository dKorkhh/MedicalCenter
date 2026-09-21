package org.example.medicalrecords.controller;

import lombok.RequiredArgsConstructor;
import org.example.medicalrecords.dto.CreateMedicalRecordRequest;
import org.example.medicalrecords.dto.MedicalRecordResponse;
import org.example.medicalrecords.service.MedicalRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @PostMapping
    public ResponseEntity<MedicalRecordResponse> createMedicalRecord(@RequestBody CreateMedicalRecordRequest request) {
        MedicalRecordResponse createdRecord = medicalRecordService.createMedicalRecord(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRecord);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalRecordResponse> getMedicalRecordById(@PathVariable String id) {
        MedicalRecordResponse record = medicalRecordService.getMedicalRecordById(id);
        return ResponseEntity.ok(record);
    }

    @GetMapping
    public ResponseEntity<List<MedicalRecordResponse>> getMedicalRecords(@RequestParam(required = false) String patientId) {
        if (patientId != null && !patientId.isBlank()) {
            return ResponseEntity.ok(medicalRecordService.getMedicalRecordsByPatientId(patientId));
        }
        return ResponseEntity.ok(medicalRecordService.getAllMedicalRecords());
    }
}
