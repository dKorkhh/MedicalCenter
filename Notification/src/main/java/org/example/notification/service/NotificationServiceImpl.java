package org.example.notification.service;

import lombok.RequiredArgsConstructor;
import org.example.notification.dto.NotificationResponse;
import org.example.notification.dto.SendNotificationRequest;
import org.example.notification.model.Notification;
import org.example.notification.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    public NotificationResponse sendNotification(SendNotificationRequest request) {
        Notification notification = Notification.builder()
                .recipientEmail(request.recipientEmail())
                .recipientPhone(request.recipientPhone())
                .notificationType(request.notificationType())
                .messageTitle(request.messageTitle())
                .messageContent(request.messageContent())
                .deliveryStatus("SENT")
                .sentAt(LocalDateTime.now())
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        return mapToResponse(savedNotification);
    }

    @Override
    public NotificationResponse getNotificationById(String id) {
        return notificationRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found with id: " + id));
    }

    @Override
    public List<NotificationResponse> getAllNotifications() {
        return notificationRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<NotificationResponse> getNotificationsByRecipientEmail(String recipientEmail) {
        return notificationRepository.findByRecipientEmail(recipientEmail)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getRecipientEmail(),
                notification.getRecipientPhone(),
                notification.getNotificationType(),
                notification.getMessageTitle(),
                notification.getMessageContent(),
                notification.getDeliveryStatus(),
                notification.getSentAt()
        );
    }
}
