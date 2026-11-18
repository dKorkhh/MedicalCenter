package org.example.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record SendNotificationRequest(
        @NotBlank @Email String recipientEmail,
        String recipientPhone,
        @NotBlank String notificationType,
        @NotBlank String messageTitle,
        @NotBlank String messageContent
) {
}
