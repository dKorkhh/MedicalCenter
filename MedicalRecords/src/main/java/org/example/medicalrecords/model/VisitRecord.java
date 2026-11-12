package org.example.medicalrecords.model;

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
@Document(collection = "visit_records")
public class VisitRecord {

    @Id
    private String id;
    private String eventId;
    private String appointmentId;
    private String timeSlotId;
    private String patientId;
    private String patientFullName;
    private String patientEmail;
    private String doctorId;
    private String doctorFullName;
    private String serviceName;
    private String diagnosis;
    private String prescription;
    private String doctorNotes;
    private LocalDateTime appointmentDateTime;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
