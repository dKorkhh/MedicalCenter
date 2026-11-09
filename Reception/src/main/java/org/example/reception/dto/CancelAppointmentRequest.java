package org.example.reception.dto;

import lombok.Builder;

@Builder
public record CancelAppointmentRequest(
        String reason
) {
}
