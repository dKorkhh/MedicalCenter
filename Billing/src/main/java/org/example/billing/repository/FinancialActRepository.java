package org.example.billing.repository;

import org.example.billing.model.FinancialAct;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialActRepository extends MongoRepository<FinancialAct, String> {

    Optional<FinancialAct> findByAppointmentId(String appointmentId);

    Optional<FinancialAct> findByActNumber(String actNumber);

    List<FinancialAct> findByPatientId(String patientId);

    List<FinancialAct> findByDoctorId(String doctorId);
}
