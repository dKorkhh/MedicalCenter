package org.example.notification.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppointmentBookedEvent {
    private String eventId;
    private String eventType;
    private String appointmentId;
    private String patientId;
    private String patientFullName;
    private String patientEmail;
    private String doctorId;
    private String doctorFullName;
    private LocalDateTime appointmentDateTime;
    private String roomNumber;
    private String invoiceId;
    private LocalDateTime timestamp;
}
