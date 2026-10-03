package org.example.medicalcenter.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record InvoiceDto(String id, String appointmentId, String patientId, BigDecimal amount, String currency,
                         String paymentStatus, LocalDateTime issuedAt, LocalDateTime paidAt) {
}
