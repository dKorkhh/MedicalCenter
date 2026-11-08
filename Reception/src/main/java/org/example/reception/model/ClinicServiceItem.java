package org.example.reception.model;

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
@Document(collection = "clinic_services")
public class ClinicServiceItem {

    @Id
    private String id;
    private String name;
    private String description;
    private String specialization;
    private BigDecimal price;
    private String currency;
    private Integer durationMinutes;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
