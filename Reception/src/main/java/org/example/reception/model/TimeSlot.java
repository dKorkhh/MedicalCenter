package org.example.reception.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "time_slots")
public class TimeSlot {

    @Id
    private String id;
    private String doctorId;
    private String scheduleId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String roomNumber;
    private TimeSlotStatus status;
    private String heldByPatientId;
    private String appointmentId;
    @Version
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
