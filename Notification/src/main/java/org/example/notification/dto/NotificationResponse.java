package org.example.notification.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record NotificationResponse(
        String id,
        String recipientEmail,
        String recipientPhone,
        String notificationType,
        String messageTitle,
        String messageContent,
        String deliveryStatus,
        LocalDateTime sentAt
) {
}
