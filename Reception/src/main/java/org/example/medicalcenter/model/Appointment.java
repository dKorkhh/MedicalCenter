package org.example.medicalcenter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "appointments")
public class Appointment {

    @Id
    private String id;
    private String patientId;
    private String patientFullName;
    private String doctorId;
    private String doctorFullName;
    private LocalDateTime appointmentDateTime;
    private String appointmentStatus;
    private String roomNumber;
    private LocalDateTime createdAt;
}
