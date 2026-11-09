package org.example.billing.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "refunds")
public class Refund {

    @Id
    private String id;
    private String eventId;
    private String appointmentId;
    private String invoiceId;
    private String patientId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String cancelledBy;
    private String reason;
    private LocalDateTime processedAt;
}
