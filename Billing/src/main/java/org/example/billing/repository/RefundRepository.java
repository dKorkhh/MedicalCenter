package org.example.billing.repository;

import org.example.billing.model.Refund;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends MongoRepository<Refund, String> {

    boolean existsByEventId(String eventId);

    boolean existsByAppointmentId(String appointmentId);

    Optional<Refund> findByAppointmentId(String appointmentId);

    List<Refund> findByPatientId(String patientId);
}
