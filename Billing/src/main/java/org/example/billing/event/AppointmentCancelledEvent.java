package org.example.billing.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record AppointmentCancelledEvent(
        String eventId,
        String eventType,
        String appointmentId,
        String timeSlotId,
        String patientId,
        String patientFullName,
        String patientEmail,
        String doctorId,
        String doctorFullName,
        LocalDateTime appointmentDateTime,
        String invoiceId,
        BigDecimal refundAmount,
        String currency,
        String cancelledBy,
        String cancellationReason,
        LocalDateTime timestamp
) {
}
