package org.example.medicalrecords.repository;

import org.example.medicalrecords.model.VisitRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VisitRecordRepository extends MongoRepository<VisitRecord, String> {

    Optional<VisitRecord> findByAppointmentId(String appointmentId);

    List<VisitRecord> findByPatientIdOrderByCompletedAtDesc(String patientId);

    List<VisitRecord> findByDoctorIdOrderByCompletedAtDesc(String doctorId);

    boolean existsByAppointmentId(String appointmentId);

    boolean existsByEventId(String eventId);
}
