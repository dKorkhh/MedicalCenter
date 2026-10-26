package org.example.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notification.event.AppointmentBookedEvent;
import org.example.notification.factory.AppointmentNotificationFactory;
import org.example.notification.model.Notification;
import org.example.notification.model.ProcessedMessage;
import org.example.notification.repository.NotificationRepository;
import org.example.notification.repository.ProcessedMessageRepository;
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

    public boolean isAlreadyProcessed(String eventId) {
        return eventId != null && processedMessageRepository.existsById(eventId);
    }

    @Transactional
    public Notification processAppointmentBooking(AppointmentBookedEvent event, String topic) {
        Notification notification = notificationFactory.createAppointmentConfirmationNotification(event);
        Notification savedNotification = notificationRepository.save(notification);

        if (event.getEventId() != null) {
            ProcessedMessage processedMessage = ProcessedMessage.builder()
                    .messageId(event.getEventId())
                    .eventType(event.getEventType())
                    .sourceTopic(topic)
                    .processedAt(LocalDateTime.now())
                    .build();
            processedMessageRepository.save(processedMessage);
        }

        return savedNotification;
    }
}
