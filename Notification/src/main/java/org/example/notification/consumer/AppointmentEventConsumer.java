package org.example.notification.consumer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.example.notification.event.AppointmentBookedEvent;
import org.example.notification.event.AppointmentCancelledEvent;
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

import java.util.function.Supplier;

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
        processConsumerEvent(
                "kafka-appointment-booked-consumer",
                event.getEventId(),
                event.getDoctorId(),
                topic,
                offset,
                traceparentHeader,
                () -> notificationProcessor.processAppointmentBooking(event, topic));
    }

    @KafkaListener(
            topics = "${app.kafka.topics.appointment-cancelled:medical.appointment.cancelled}",
            groupId = "${spring.kafka.consumer.group-id:notification-service-group}",
            containerFactory = "cancelledAppointmentKafkaListenerContainerFactory"
    )
    public void handleAppointmentCancelled(
            AppointmentCancelledEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(name = "traceparent", required = false) Object traceparentHeader) {
        processConsumerEvent(
                "kafka-appointment-cancelled-consumer",
                event.eventId(),
                event.doctorId(),
                topic,
                offset,
                traceparentHeader,
                () -> notificationProcessor.processAppointmentCancellation(event, topic));
    }

    private void processConsumerEvent(
            String spanName,
            String eventId,
            String doctorId,
            String topic,
            long offset,
            Object traceparentHeader,
            Supplier<Notification> notificationAction) {
        String traceparent = traceContextExtractor.extractTraceparent(traceparentHeader);

        try (TraceContextSession session = traceContextExtractor.startConsumerSpan(spanName, traceparent)) {
            log.info("Received event: id={}, topic={}, offset={}, traceparent={}",
                    eventId, topic, offset, traceparent);

            if (notificationProcessor.isAlreadyProcessed(eventId)) {
                log.warn("Idempotent Consumer: Duplicate event detected (eventId={}). Skipping business execution.", eventId);
                return;
            }

            verifyFailureSimulation(eventId, doctorId);

            try {
                Notification notification = notificationAction.get();
                log.info("Idempotent Consumer: Successfully processed eventId={}, created notificationId={}",
                        eventId, notification.getId());
            } catch (Exception exception) {
                session.recordFailure(exception);
                throw exception;
            }
        }
    }

    private void verifyFailureSimulation(String eventId, String doctorId) {
        if (simulateFailure || "FAIL_SIMULATION".equals(doctorId)) {
            log.error("Artificial consumer failure triggered for eventId={}", eventId);
            throw new RuntimeException("Artificial failure triggered for eventId=" + eventId);
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
