package org.example.notification.factory;

import org.example.notification.event.AppointmentBookedEvent;
import org.example.notification.model.Notification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AppointmentNotificationFactory {

    private static final String DEFAULT_PHONE = "+380501234567";
    private static final String NOTIFICATION_TYPE = "APPOINTMENT_CONFIRMATION";
    private static final String CONFIRMATION_TITLE = "Запис підтверджено";
    private static final String CONFIRMATION_TEMPLATE = "Шановний(а) %s! Ваш запис до лікаря %s підтверджено на %s. Кабінет: %s";
    private static final String STATUS_SENT = "SENT";

    public Notification createAppointmentConfirmationNotification(AppointmentBookedEvent event) {
        return Notification.builder()
                .recipientEmail(resolveRecipientEmail(event))
                .recipientPhone(DEFAULT_PHONE)
                .notificationType(NOTIFICATION_TYPE)
                .messageTitle(CONFIRMATION_TITLE)
                .messageContent(buildMessageContent(event))
                .deliveryStatus(STATUS_SENT)
                .sentAt(LocalDateTime.now())
                .build();
    }

    private String resolveRecipientEmail(AppointmentBookedEvent event) {
        if (event.getPatientEmail() != null && !event.getPatientEmail().isBlank()) {
            return event.getPatientEmail();
        }
        return "patient-" + event.getPatientId() + "@medicalcenter.local";
    }

    private String buildMessageContent(AppointmentBookedEvent event) {
        return String.format(CONFIRMATION_TEMPLATE,
                event.getPatientFullName(),
                event.getDoctorFullName(),
                event.getAppointmentDateTime(),
                event.getRoomNumber());
    }
}
