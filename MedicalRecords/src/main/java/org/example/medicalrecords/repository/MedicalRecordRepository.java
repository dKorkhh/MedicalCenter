package org.example.medicalrecords.repository;

import org.example.medicalrecords.model.MedicalRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalRecordRepository extends MongoRepository<MedicalRecord, String> {

    List<MedicalRecord> findByPatientId(String patientId);

    List<MedicalRecord> findByDoctorId(String doctorId);

    List<MedicalRecord> findByAppointmentId(String appointmentId);
}
