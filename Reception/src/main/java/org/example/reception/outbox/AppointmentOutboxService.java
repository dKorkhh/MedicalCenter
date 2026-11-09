package org.example.reception.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.reception.event.AppointmentBookedEvent;
import org.example.reception.event.AppointmentCancelledEvent;
import org.example.reception.event.AppointmentCompletedEvent;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentOutboxService {

    private static final String APPOINTMENT_AGGREGATE_TYPE = "Appointment";

    private final OutboxMessageRepository outboxMessageRepository;
    private final ObjectMapper objectMapper;
    private final TraceContextProvider traceContextProvider;

    public OutboxMessage recordAppointmentBookedEvent(AppointmentBookedEvent event) {
        return saveOutboxMessage(event.getEventId(), event.getAppointmentId(), event.getEventType(), event);
    }

    public OutboxMessage recordAppointmentCancelledEvent(AppointmentCancelledEvent event) {
        return saveOutboxMessage(event.eventId(), event.appointmentId(), event.eventType(), event);
    }

    public OutboxMessage recordAppointmentCompletedEvent(AppointmentCompletedEvent event) {
        return saveOutboxMessage(event.eventId(), event.appointmentId(), event.eventType(), event);
    }

    private OutboxMessage saveOutboxMessage(String eventId, String appointmentId, String eventType, Object event) {
        String traceparent = traceContextProvider.captureCurrentTraceparent();
        String payload = serializeEventPayload(event);

        OutboxMessage outboxMessage = OutboxMessage.builder()
                .id(eventId)
                .aggregateType(APPOINTMENT_AGGREGATE_TYPE)
                .aggregateId(appointmentId)
                .eventType(eventType)
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
