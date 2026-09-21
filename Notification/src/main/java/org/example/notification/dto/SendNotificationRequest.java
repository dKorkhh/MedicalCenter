package org.example.notification.dto;

import lombok.Builder;

@Builder
public record SendNotificationRequest(
        String recipientEmail,
        String recipientPhone,
        String notificationType,
        String messageTitle,
        String messageContent
) {
}
