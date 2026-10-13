package org.example.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.notification.event.AppointmentBookedEvent;
import org.example.notification.model.Notification;
import org.example.notification.model.NotificationDeadLetter;
import org.example.notification.model.ProcessedMessage;
import org.example.notification.repository.NotificationDeadLetterRepository;
import org.example.notification.repository.NotificationRepository;
import org.example.notification.repository.ProcessedMessageRepository;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentEventConsumer {

    private final NotificationRepository notificationRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final NotificationDeadLetterRepository notificationDeadLetterRepository;
    private final ObjectMapper objectMapper;

    @Getter
    @Setter
    private volatile boolean simulateFailure = false;

    @RetryableTopic(attempts = "3", backOff = @BackOff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltStrategy = DltStrategy.FAIL_ON_ERROR, include = {RuntimeException.class})
    @KafkaListener(topics = "${app.kafka.topics.appointment-booked:medical.appointment.booked}",
            groupId = "${spring.kafka.consumer.group-id:notification-service-group}")
    public void handleAppointmentBooked(AppointmentBookedEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic, @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Received event: id={}, topic={}, offset={}", event.getEventId(), topic, offset);

        if (event.getEventId() != null && processedMessageRepository.existsById(event.getEventId())) {
            log.warn("Idempotent Consumer: Duplicate event detected (eventId={}). Skipping business execution.", event.getEventId());
            return;
        }

        if (simulateFailure || "FAIL_SIMULATION".equals(event.getDoctorId())) {
            log.error("Artificial consumer failure triggered for eventId={}", event.getEventId());
            throw new RuntimeException("Artificial failure triggered for eventId=" + event.getEventId());
        }

        Notification notification = Notification.builder().recipientEmail(event.getPatientEmail() != null ? event.getPatientEmail() : ("patient-" + event.getPatientId() + "@medicalcenter.local")).recipientPhone("+380501234567").notificationType("APPOINTMENT_CONFIRMATION").messageTitle("Запис підтверджено").messageContent(String.format("Шановний(а) %s! Ваш запис до лікаря %s підтверджено на %s. Кабінет: %s", event.getPatientFullName(), event.getDoctorFullName(), event.getAppointmentDateTime(), event.getRoomNumber())).deliveryStatus("SENT").sentAt(LocalDateTime.now()).build();

        notificationRepository.save(notification);

        if (event.getEventId() != null) {
            ProcessedMessage processedMessage = ProcessedMessage.builder().messageId(event.getEventId()).eventType(event.getEventType()).sourceTopic(topic).processedAt(LocalDateTime.now()).build();
            processedMessageRepository.save(processedMessage);
        }

        log.info("Idempotent Consumer: Successfully processed eventId={}, created notificationId={}", event.getEventId(), notification.getId());
    }

    @DltHandler
    public void handleDlt(AppointmentBookedEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic, @Header(KafkaHeaders.OFFSET) long offset, @Header(name = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage) {
        log.error("Dead Letter Topic (DLT) reached: eventId={}, fromTopic={}, offset={}, error={}", event.getEventId(), topic, offset, exceptionMessage);

        try {
            NotificationDeadLetter deadLetter = NotificationDeadLetter.builder()
                    .eventId(event.getEventId())
                    .originalTopic(topic)
                    .offset(offset)
                    .errorMessage(exceptionMessage)
                    .rawPayload(objectMapper.writeValueAsString(event))
                    .failedAt(LocalDateTime.now()).build();
            notificationDeadLetterRepository.save(deadLetter);
            log.info("Recorded DLT failure event to database: eventId={}", event.getEventId());
        } catch (Exception ex) {
            log.error("Failed to record DLT failure event: {}", ex.getMessage());
        }
    }
}
