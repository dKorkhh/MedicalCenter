package org.example.notification.service;

import io.micrometer.core.instrument.LongTaskTimer;
import lombok.RequiredArgsConstructor;
import org.example.notification.dto.NotificationResponse;
import org.example.notification.dto.SendNotificationRequest;
import org.example.notification.mapper.NotificationMapper;
import org.example.notification.model.Notification;
import org.example.notification.repository.NotificationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final EmailService emailService;
    private final SendingMetricsService sendingMetricsService;

    @Override
    public NotificationResponse sendNotification(SendNotificationRequest request) {
        LongTaskTimer.Sample sample = sendingMetricsService.reportTimer().start();
        try {
            if (request.recipientEmail() != null && !request.recipientEmail().isBlank()) {
                emailService.sendEmail(
                        request.recipientEmail(),
                        request.messageTitle(),
                        request.messageContent()
                );
            }
            Notification notification = notificationMapper.toEntity(request);
            Notification savedNotification = notificationRepository.save(notification);
            return notificationMapper.toResponse(savedNotification);
        } finally {
            sample.stop();
        }
    }

    @Override
    public NotificationResponse getNotificationById(String id) {
        return notificationRepository.findById(id)
                .map(notificationMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found with id: " + id));
    }

    @Override
    public List<NotificationResponse> getAllNotifications() {
        return notificationRepository.findAll()
                .stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @Override
    public List<NotificationResponse> getNotificationsByRecipientEmail(String recipientEmail) {
        return notificationRepository.findByRecipientEmail(recipientEmail)
                .stream()
                .map(notificationMapper::toResponse)
                .toList();
    }
}
