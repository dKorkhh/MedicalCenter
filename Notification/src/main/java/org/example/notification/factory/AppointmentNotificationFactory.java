package org.example.notification.factory;

import org.example.notification.event.AppointmentBookedEvent;
import org.example.notification.event.AppointmentCancelledEvent;
import org.example.notification.model.Notification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AppointmentNotificationFactory {

    private static final String DEFAULT_PHONE = "+380501234567";
    private static final String CONFIRMATION_TYPE = "APPOINTMENT_CONFIRMATION";
    private static final String CANCELLATION_TYPE = "APPOINTMENT_CANCELLATION";
    private static final String CONFIRMATION_TITLE = "Запис підтверджено";
    private static final String CANCELLATION_TITLE = "Запис скасовано";
    private static final String CONFIRMATION_TEMPLATE = "Шановний(а) %s! Ваш запис до лікаря %s підтверджено на %s. Кабінет: %s";
    private static final String CANCELLATION_TEMPLATE = "Шановний(а) %s! Ваш запис до лікаря %s на %s скасовано.";
    private static final String STATUS_SENT = "SENT";

    public Notification createAppointmentConfirmationNotification(AppointmentBookedEvent event) {
        return Notification.builder()
                .recipientEmail(resolveRecipientEmail(event.getPatientEmail(), event.getPatientId()))
                .recipientPhone(DEFAULT_PHONE)
                .notificationType(CONFIRMATION_TYPE)
                .messageTitle(CONFIRMATION_TITLE)
                .messageContent(buildConfirmationMessageContent(event))
                .deliveryStatus(STATUS_SENT)
                .sentAt(LocalDateTime.now())
                .build();
    }

    public Notification createAppointmentCancellationNotification(AppointmentCancelledEvent event) {
        return Notification.builder()
                .recipientEmail(resolveRecipientEmail(event.patientEmail(), event.patientId()))
                .recipientPhone(DEFAULT_PHONE)
                .notificationType(CANCELLATION_TYPE)
                .messageTitle(CANCELLATION_TITLE)
                .messageContent(buildCancellationMessageContent(event))
                .deliveryStatus(STATUS_SENT)
                .sentAt(LocalDateTime.now())
                .build();
    }

    private String resolveRecipientEmail(String patientEmail, String patientId) {
        if (patientEmail != null && !patientEmail.isBlank()) {
            return patientEmail;
        }
        return "patient-" + patientId + "@medicalcenter.local";
    }

    private String buildConfirmationMessageContent(AppointmentBookedEvent event) {
        return String.format(CONFIRMATION_TEMPLATE,
                event.getPatientFullName(),
                event.getDoctorFullName(),
                event.getAppointmentDateTime(),
                event.getRoomNumber());
    }

    private String buildCancellationMessageContent(AppointmentCancelledEvent event) {
        return String.format(CANCELLATION_TEMPLATE,
                event.patientFullName(),
                event.doctorFullName(),
                event.appointmentDateTime());
    }
}
