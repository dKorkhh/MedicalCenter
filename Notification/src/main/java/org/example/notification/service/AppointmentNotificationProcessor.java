package org.example.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notification.event.AppointmentBookedEvent;
import org.example.notification.event.AppointmentCancelledEvent;
import org.example.notification.factory.AppointmentNotificationFactory;
import org.example.notification.model.Notification;
import org.example.notification.model.ProcessedMessage;
import org.example.notification.repository.NotificationRepository;
import org.example.notification.repository.ProcessedMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentNotificationProcessor {

    private final NotificationRepository notificationRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final AppointmentNotificationFactory notificationFactory;
    private final EmailService emailService;

    public boolean isAlreadyProcessed(String eventId) {
        return eventId != null && processedMessageRepository.existsById(eventId);
    }

    @Transactional
    public Notification processAppointmentBooking(AppointmentBookedEvent event, String topic) {
        Notification notification = notificationFactory.createAppointmentConfirmationNotification(event);
        return saveNotificationAndProcessedMessage(notification, event.getEventId(), event.getEventType(), topic);
    }

    @Transactional
    public Notification processAppointmentCancellation(AppointmentCancelledEvent event, String topic) {
        Notification notification = notificationFactory.createAppointmentCancellationNotification(event);
        dispatchEmailSafely(notification);
        return saveNotificationAndProcessedMessage(notification, event.eventId(), event.eventType(), topic);
    }

    private Notification saveNotificationAndProcessedMessage(
            Notification notification,
            String eventId,
            String eventType,
            String topic) {
        Notification savedNotification = notificationRepository.save(notification);
        if (eventId != null) {
            ProcessedMessage processedMessage = ProcessedMessage.builder()
                    .messageId(eventId)
                    .eventType(eventType)
                    .sourceTopic(topic)
                    .processedAt(LocalDateTime.now())
                    .build();
            processedMessageRepository.save(processedMessage);
        }
        return savedNotification;
    }

    private void dispatchEmailSafely(Notification notification) {
        if (emailService == null || notification.getRecipientEmail() == null || notification.getRecipientEmail().isBlank()) {
            return;
        }
        try {
            emailService.sendEmail(
                    notification.getRecipientEmail(),
                    notification.getMessageTitle(),
                    notification.getMessageContent());
        } catch (Exception ex) {
            log.warn("SMTP delivery skipped or failed for {}: {}", notification.getRecipientEmail(), ex.getMessage());
        }
    }
}
