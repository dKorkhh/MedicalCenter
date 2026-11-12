package org.example.billing.service;

import org.example.billing.dto.FinancialActResponse;
import org.example.billing.dto.PaymentSnapshotResponse;
import org.example.billing.event.AppointmentCompletedEvent;

import java.util.List;

public interface FinancialActService {

    FinancialActResponse processAppointmentCompletion(AppointmentCompletedEvent event);

    FinancialActResponse getFinancialActById(String id);

    FinancialActResponse getFinancialActByAppointmentId(String appointmentId);

    List<FinancialActResponse> getAllFinancialActs();

    List<FinancialActResponse> getFinancialActsByPatientId(String patientId);

    List<FinancialActResponse> getFinancialActsByDoctorId(String doctorId);

    PaymentSnapshotResponse getPaymentSnapshotByAppointmentId(String appointmentId);
}
