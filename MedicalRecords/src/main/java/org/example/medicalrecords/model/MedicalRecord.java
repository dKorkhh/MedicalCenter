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
@Document(collection = "medical_records")
public class MedicalRecord {

    @Id
    private String id;
    private String patientId;
    private String doctorId;
    private String appointmentId;
    private String diagnosis;
    private String prescription;
    private String doctorNotes;
    private LocalDateTime recordDate;
}
