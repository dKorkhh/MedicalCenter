package org.example.reception.idempotency;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "idempotency_keys")
public class IdempotencyRecord {

    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String COMPLETED = "COMPLETED";

    @Id
    private String key;
    private String requestHash;
    private String status;
    private String resourceId;

    @Indexed(expireAfter = "24h")
    private Instant createdAt;
}
