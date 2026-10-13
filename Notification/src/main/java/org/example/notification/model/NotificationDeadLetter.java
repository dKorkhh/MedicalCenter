package org.example.notification.model;

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
@Document(collection = "notification_dead_letters")
public class NotificationDeadLetter {

    @Id
    private String id;
    private String eventId;
    private String originalTopic;
    private long offset;
    private String errorMessage;
    private String rawPayload;
    private LocalDateTime failedAt;
}
