package org.example.reception.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.reception.event.AppointmentBookedEvent;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentOutboxService {

    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;
    private final TraceContextProvider traceContextProvider;

    public OutboxMessage recordAppointmentBookedEvent(AppointmentBookedEvent event) {
        String traceparent = traceContextProvider.captureCurrentTraceparent();
        String payload = serializeEventPayload(event);

        OutboxMessage outboxMessage = OutboxMessage.builder()
                .id(event.getEventId())
                .aggregateType("Appointment")
                .aggregateId(event.getAppointmentId())
                .eventType(event.getEventType())
                .payload(payload)
                .status(OutboxStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .traceparent(traceparent)
                .build();

        return outboxMessageRepository.save(outboxMessage);
    }

    private String serializeEventPayload(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to serialize outbox event: " + ex.getMessage(), ex);
        }
    }
}
