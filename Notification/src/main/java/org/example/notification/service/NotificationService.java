package org.example.notification.service;

import org.example.notification.dto.NotificationResponse;
import org.example.notification.dto.SendNotificationRequest;

import java.util.List;

public interface NotificationService {

    NotificationResponse sendNotification(SendNotificationRequest request);

    NotificationResponse getNotificationById(String id);

    List<NotificationResponse> getAllNotifications();

    List<NotificationResponse> getNotificationsByRecipientEmail(String recipientEmail);
}
