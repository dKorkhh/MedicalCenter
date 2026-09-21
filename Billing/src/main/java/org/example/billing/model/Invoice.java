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
@Document(collection = "invoices")
public class Invoice {

    @Id
    private String id;
    private String appointmentId;
    private String patientId;
    private BigDecimal amount;
    private String currency;
    private String paymentStatus;
    private LocalDateTime issuedAt;
    private LocalDateTime paidAt;
}
