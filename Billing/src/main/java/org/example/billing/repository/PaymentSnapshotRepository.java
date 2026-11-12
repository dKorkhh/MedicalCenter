package org.example.billing.repository;

import org.example.billing.model.PaymentSnapshot;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentSnapshotRepository extends MongoRepository<PaymentSnapshot, String> {

    Optional<PaymentSnapshot> findByAppointmentId(String appointmentId);

    Optional<PaymentSnapshot> findByInvoiceId(String invoiceId);

    List<PaymentSnapshot> findByPatientId(String patientId);
}
