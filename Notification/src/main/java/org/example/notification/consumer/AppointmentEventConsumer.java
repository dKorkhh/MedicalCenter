package org.example.notification.consumer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.notification.event.AppointmentBookedEvent;
import org.example.notification.model.Notification;
import org.example.notification.service.AppointmentNotificationProcessor;
import org.example.notification.service.NotificationDeadLetterRecorder;
import org.example.notification.tracing.KafkaTraceContextExtractor;
import org.example.notification.tracing.TraceContextSession;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentEventConsumer {

    private final AppointmentNotificationProcessor notificationProcessor;
    private final KafkaTraceContextExtractor traceContextExtractor;
    private final NotificationDeadLetterRecorder deadLetterRecorder;

    @Getter
    @Setter
    private volatile boolean simulateFailure = false;

    @RetryableTopic(attempts = "3", backOff = @BackOff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltStrategy = DltStrategy.FAIL_ON_ERROR, include = {RuntimeException.class})
    @KafkaListener(topics = "${app.kafka.topics.appointment-booked:medical.appointment.booked}",
            groupId = "${spring.kafka.consumer.group-id:notification-service-group}")
    public void handleAppointmentBooked(
            AppointmentBookedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(name = "traceparent", required = false) Object traceparentHeader) {

        String traceparent = traceContextExtractor.extractTraceparent(traceparentHeader);

        try (TraceContextSession session = traceContextExtractor.startConsumerSpan("kafka-appointment-booked-consumer", traceparent)) {
            log.info("Received event: id={}, topic={}, offset={}, traceparent={}",
                    event.getEventId(), topic, offset, traceparent);

            if (notificationProcessor.isAlreadyProcessed(event.getEventId())) {
                log.warn("Idempotent Consumer: Duplicate event detected (eventId={}). Skipping business execution.", event.getEventId());
                return;
            }

            verifyFailureSimulation(event);

            try {
                Notification notification = notificationProcessor.processAppointmentBooking(event, topic);
                log.info("Idempotent Consumer: Successfully processed eventId={}, created notificationId={}",
                        event.getEventId(), notification.getId());
            } catch (Exception exception) {
                session.recordFailure(exception);
                throw exception;
            }
        }
    }

    private void verifyFailureSimulation(AppointmentBookedEvent event) {
        if (simulateFailure || "FAIL_SIMULATION".equals(event.getDoctorId())) {
            log.error("Artificial consumer failure triggered for eventId={}", event.getEventId());
            throw new RuntimeException("Artificial failure triggered for eventId=" + event.getEventId());
        }
    }

    @DltHandler
    public void handleDlt(
            AppointmentBookedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(name = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage) {
        deadLetterRecorder.recordFailure(event, topic, offset, exceptionMessage);
    }
}
