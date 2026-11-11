package org.example.reception.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
public record CreateAppointmentRequest(String patientId, String patientFullName, String patientEmail, String doctorId,
                                       String doctorFullName, @NotBlank String timeSlotId,
                                       LocalDateTime appointmentDateTime, String roomNumber, String invoiceId) {
}
