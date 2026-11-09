package org.example.billing.service;

import org.example.billing.dto.RefundResponse;
import org.example.billing.event.AppointmentCancelledEvent;

import java.util.List;

public interface RefundService {

    RefundResponse processAppointmentCancellationRefund(AppointmentCancelledEvent event);

    RefundResponse getRefundById(String id);

    List<RefundResponse> getAllRefunds();

    List<RefundResponse> getRefundsByPatientId(String patientId);
}
