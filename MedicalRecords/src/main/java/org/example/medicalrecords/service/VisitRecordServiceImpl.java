package org.example.medicalrecords.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.medicalrecords.dto.VisitRecordResponse;
import org.example.medicalrecords.event.AppointmentCompletedEvent;
import org.example.medicalrecords.model.MedicalRecord;
import org.example.medicalrecords.model.VisitRecord;
import org.example.medicalrecords.repository.MedicalRecordRepository;
import org.example.medicalrecords.repository.VisitRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisitRecordServiceImpl implements VisitRecordService {

    private final VisitRecordRepository visitRecordRepository;
    private final MedicalRecordRepository medicalRecordRepository;

    @Override
    @Transactional
    public VisitRecord processAppointmentCompleted(AppointmentCompletedEvent event) {
        Optional<VisitRecord> existingRecord = visitRecordRepository.findByAppointmentId(event.appointmentId());
        if (existingRecord.isPresent()) {
            log.info("VisitRecord already exists for appointmentId={}", event.appointmentId());
            return existingRecord.get();
        }

        VisitRecord visitRecord = buildVisitRecordFromEvent(event);
        VisitRecord savedVisitRecord = visitRecordRepository.save(visitRecord);

        synchronizeGeneralMedicalRecord(event);

        log.info("Successfully created VisitRecord id={} for appointmentId={}",
                savedVisitRecord.getId(), event.appointmentId());
        return savedVisitRecord;
    }

    @Override
    public VisitRecordResponse getVisitRecordByAppointmentId(String appointmentId) {
        VisitRecord visitRecord = visitRecordRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Visit record not found for appointmentId: " + appointmentId));
        return mapToResponse(visitRecord);
    }

    @Override
    public List<VisitRecordResponse> getVisitRecordsByPatientId(String patientId) {
        return visitRecordRepository.findByPatientIdOrderByCompletedAtDesc(patientId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<VisitRecordResponse> getVisitRecordsByDoctorId(String doctorId) {
        return visitRecordRepository.findByDoctorIdOrderByCompletedAtDesc(doctorId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private VisitRecord buildVisitRecordFromEvent(AppointmentCompletedEvent event) {
        LocalDateTime now = LocalDateTime.now();
        return VisitRecord.builder()
                .eventId(event.eventId())
                .appointmentId(event.appointmentId())
                .timeSlotId(event.timeSlotId())
                .patientId(event.patientId())
                .patientFullName(event.patientFullName())
                .patientEmail(event.patientEmail())
                .doctorId(event.doctorId())
                .doctorFullName(event.doctorFullName())
                .serviceName(event.serviceName())
                .diagnosis(event.diagnosis())
                .prescription(event.prescription())
                .doctorNotes(event.doctorNotes())
                .appointmentDateTime(event.appointmentDateTime())
                .completedAt(event.completedAt() != null ? event.completedAt() : now)
                .createdAt(now)
                .build();
    }

    private void synchronizeGeneralMedicalRecord(AppointmentCompletedEvent event) {
        boolean exists = medicalRecordRepository.existsByAppointmentId(event.appointmentId());
        if (!exists) {
            MedicalRecord medicalRecord = MedicalRecord.builder()
                    .patientId(event.patientId())
                    .doctorId(event.doctorId())
                    .appointmentId(event.appointmentId())
                    .diagnosis(event.diagnosis())
                    .prescription(event.prescription())
                    .doctorNotes(event.doctorNotes())
                    .recordDate(event.completedAt() != null ? event.completedAt() : LocalDateTime.now())
                    .build();
            medicalRecordRepository.save(medicalRecord);
        }
    }

    private VisitRecordResponse mapToResponse(VisitRecord visitRecord) {
        return VisitRecordResponse.builder()
                .id(visitRecord.getId())
                .eventId(visitRecord.getEventId())
                .appointmentId(visitRecord.getAppointmentId())
                .timeSlotId(visitRecord.getTimeSlotId())
                .patientId(visitRecord.getPatientId())
                .patientFullName(visitRecord.getPatientFullName())
                .patientEmail(visitRecord.getPatientEmail())
                .doctorId(visitRecord.getDoctorId())
                .doctorFullName(visitRecord.getDoctorFullName())
                .serviceName(visitRecord.getServiceName())
                .diagnosis(visitRecord.getDiagnosis())
                .prescription(visitRecord.getPrescription())
                .doctorNotes(visitRecord.getDoctorNotes())
                .appointmentDateTime(visitRecord.getAppointmentDateTime())
                .completedAt(visitRecord.getCompletedAt())
                .createdAt(visitRecord.getCreatedAt())
                .build();
    }
}
