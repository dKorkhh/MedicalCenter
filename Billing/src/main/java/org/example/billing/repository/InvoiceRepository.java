package org.example.billing.repository;

import org.example.billing.model.Invoice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends MongoRepository<Invoice, String> {

    List<Invoice> findByPatientId(String patientId);

    List<Invoice> findByAppointmentId(String appointmentId);
}
