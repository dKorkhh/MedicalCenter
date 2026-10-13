package org.example.reception.outbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "outbox_messages")
public class OutboxMessage {

    @Id
    private String id;

    private String aggregateType;
    private String aggregateId;
    private String eventType;

    private String payload;

    @Indexed
    private OutboxStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    private int retryCount;
    private String lastError;
}
