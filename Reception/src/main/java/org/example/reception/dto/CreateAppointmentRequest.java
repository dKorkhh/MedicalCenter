package org.example.reception.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder(toBuilder = true)
public record CreateAppointmentRequest(String patientId, String patientFullName, String patientEmail,
                                       @NotBlank String doctorId, String doctorFullName,
                                       String timeSlotId,
                                       @NotNull LocalDateTime appointmentDateTime, String roomNumber,
                                       String invoiceId) {
}
