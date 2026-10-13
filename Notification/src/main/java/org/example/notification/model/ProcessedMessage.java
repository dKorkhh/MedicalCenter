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
@Document(collection = "processed_messages")
public class ProcessedMessage {
    @Id
    private String messageId;

    private String eventType;
    private String sourceTopic;
    private LocalDateTime processedAt;
}
