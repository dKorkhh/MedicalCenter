package org.example.reception.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.outbox.publisher.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxEventPublisher {

    private final OutboxMessageRepository outboxMessageRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.topics.appointment-booked:medical.appointment.booked}")
    private String defaultTopic = "medical.appointment.booked";

    @Value("${app.outbox.max-retries:5}")
    private int maxRetries = 5;

    @Scheduled(fixedDelayString = "${app.outbox.polling-interval-ms:2000}")
    public void publishPendingEvents() {
        List<OutboxMessage> pendingMessages = outboxMessageRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        if (pendingMessages.isEmpty()) {
            return;
        }

        log.debug("Found {} pending outbox messages to publish", pendingMessages.size());

        for (OutboxMessage message : pendingMessages) {
            publishMessage(message);
        }
    }

    public void publishMessage(OutboxMessage message) {
        String topic = defaultTopic;
        String key = message.getAggregateId() != null ? message.getAggregateId() : message.getId();

        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, message.getPayload());
            if (message.getTraceparent() != null && !message.getTraceparent().isBlank()) {
                record.headers().add("traceparent", message.getTraceparent().getBytes(StandardCharsets.UTF_8));
            }

            kafkaTemplate.send(record)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            message.setStatus(OutboxStatus.PROCESSED);
                            message.setProcessedAt(LocalDateTime.now());
                            outboxMessageRepository.save(message);
                            log.info("Outbox message id={} (aggregateId={}) successfully published to topic={} with traceparent={}",
                                    message.getId(), message.getAggregateId(), topic, message.getTraceparent());
                        } else {
                            handleFailure(message, ex);
                        }
                    });
        } catch (Exception ex) {
            handleFailure(message, ex);
        }
    }

    private void handleFailure(OutboxMessage message, Throwable ex) {
        log.error("Failed to publish outbox message id={}: {}", message.getId(), ex.getMessage(), ex);
        message.setRetryCount(message.getRetryCount() + 1);
        message.setLastError(ex.getMessage());
        if (message.getRetryCount() >= maxRetries) {
            message.setStatus(OutboxStatus.FAILED);
            log.error("Outbox message id={} marked as FAILED after {} retries", message.getId(), maxRetries);
        }
        outboxMessageRepository.save(message);
    }
}
