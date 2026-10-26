package org.example.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notification.event.AppointmentBookedEvent;
import org.example.notification.model.NotificationDeadLetter;
import org.example.notification.repository.NotificationDeadLetterRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDeadLetterRecorder {

    private final NotificationDeadLetterRepository notificationDeadLetterRepository;
    private final ObjectMapper objectMapper;

    public void recordFailure(AppointmentBookedEvent event, String topic, long offset, String errorMessage) {
        log.error("Dead Letter Topic (DLT) reached: eventId={}, fromTopic={}, offset={}, error={}",
                event.getEventId(), topic, offset, errorMessage);

        try {
            NotificationDeadLetter deadLetter = NotificationDeadLetter.builder()
                    .eventId(event.getEventId())
                    .originalTopic(topic)
                    .offset(offset)
                    .errorMessage(errorMessage)
                    .rawPayload(objectMapper.writeValueAsString(event))
                    .failedAt(LocalDateTime.now())
                    .build();

            notificationDeadLetterRepository.save(deadLetter);
            log.info("Recorded DLT failure event to database: eventId={}", event.getEventId());
        } catch (Exception exception) {
            log.error("Failed to record DLT failure event: {}", exception.getMessage());
        }
    }
}
