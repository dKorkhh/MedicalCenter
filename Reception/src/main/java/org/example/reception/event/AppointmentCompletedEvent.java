package org.example.reception.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record AppointmentCompletedEvent(
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
        String roomNumber,
        String serviceName,
        BigDecimal amount,
        String currency,
        String invoiceId,
        String diagnosis,
        String prescription,
        String doctorNotes,
        LocalDateTime completedAt,
        LocalDateTime timestamp
) {
}
