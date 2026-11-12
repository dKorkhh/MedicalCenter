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
@Document(collection = "financial_acts")
public class FinancialAct {

    @Id
    private String id;
    private String actNumber;
    private String appointmentId;
    private String patientId;
    private String patientFullName;
    private String doctorId;
    private String doctorFullName;
    private String invoiceId;
    private String serviceName;
    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDateTime issuedAt;
    private LocalDateTime closedAt;
}
