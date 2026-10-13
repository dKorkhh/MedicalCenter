package org.example.notification.consumer;

import org.example.notification.event.AppointmentBookedEvent;
import org.example.notification.model.Notification;
import org.example.notification.model.NotificationDeadLetter;
import org.example.notification.model.ProcessedMessage;
import org.example.notification.repository.NotificationDeadLetterRepository;
import org.example.notification.repository.NotificationRepository;
import org.example.notification.repository.ProcessedMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {"spring.data.mongodb.auto-index-creation=false", "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}", "app.kafka.topics.appointment-booked=medical.appointment.booked"})
@EmbeddedKafka(partitions = 1, topics = {"medical.appointment.booked", "medical.appointment.booked-retry-0", "medical.appointment.booked-retry-1", "medical.appointment.booked-dlt"})
@DirtiesContext
class NotificationKafkaConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    private NotificationRepository notificationRepository;

    @MockitoBean
    private ProcessedMessageRepository processedMessageRepository;

    @MockitoBean
    private NotificationDeadLetterRepository notificationDeadLetterRepository;

    @Value("${app.kafka.topics.appointment-booked:medical.appointment.booked}")
    private String topic;

    private final ConcurrentHashMap<String, ProcessedMessage> processedStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Notification> notificationStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, NotificationDeadLetter> dltStore = new ConcurrentHashMap<>();
    private final AtomicInteger businessExecutionCount = new AtomicInteger(0);

    @BeforeEach
    void setUp() {
        processedStore.clear();
        notificationStore.clear();
        dltStore.clear();
        businessExecutionCount.set(0);

        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            return processedStore.containsKey(id);
        }).when(processedMessageRepository).existsById(any(String.class));

        doAnswer(invocation -> {
            ProcessedMessage pm = invocation.getArgument(0);
            processedStore.put(pm.getMessageId(), pm);
            return pm;
        }).when(processedMessageRepository).save(any(ProcessedMessage.class));

        doAnswer(invocation -> {
            Notification n = invocation.getArgument(0);
            if (n.getId() == null) {
                n.setId(UUID.randomUUID().toString());
            }
            notificationStore.put(n.getId(), n);
            businessExecutionCount.incrementAndGet();
            return n;
        }).when(notificationRepository).save(any(Notification.class));

        doAnswer(invocation -> {
            NotificationDeadLetter dlt = invocation.getArgument(0);
            if (dlt.getId() == null) {
                dlt.setId(UUID.randomUUID().toString());
            }
            dltStore.put(dlt.getEventId(), dlt);
            return dlt;
        }).when(notificationDeadLetterRepository).save(any(NotificationDeadLetter.class));
    }

    @Test
    void testIdempotentConsumer_duplicateEvents_processedOnlyOnce() throws Exception {
        String eventId = "idemp-" + UUID.randomUUID();
        AppointmentBookedEvent event = AppointmentBookedEvent.builder()
                .eventId(eventId)
                .eventType("APPOINTMENT_BOOKED")
                .appointmentId("101")
                .patientId("1")
                .patientFullName("Test user")
                .patientEmail("test@gmail.com")
                .doctorId("1")
                .doctorFullName("Test doc")
                .appointmentDateTime(LocalDateTime.now().plusDays(1))
                .roomNumber("204")
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(topic, event.getAppointmentId(), event).get();

        Thread.sleep(200);
        assertEquals(1, businessExecutionCount.get());

        kafkaTemplate.send(topic, event.getAppointmentId(), event).get();
        Thread.sleep(2000);

        assertEquals(1, businessExecutionCount.get());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void testRetryAndDeadLetterTopicRouting() throws Exception {
        String eventId = "dlt-" + UUID.randomUUID();
        AppointmentBookedEvent poisonPillEvent = AppointmentBookedEvent.builder()
                .eventId(eventId)
                .eventType("APPOINTMENT_BOOKED")
                .appointmentId("999")
                .patientId("error")
                .patientFullName("Error")
                .patientEmail("error@gmail.com")
                .doctorId("FAIL_SIMULATION")
                .doctorFullName("Error doc")
                .appointmentDateTime(LocalDateTime.now().plusDays(1))
                .roomNumber("999")
                .timestamp(LocalDateTime.now())
                .build();

        kafkaTemplate.send(topic, poisonPillEvent.getAppointmentId(), poisonPillEvent).get();

        long deadline = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < deadline && !dltStore.containsKey(eventId)) {
            Thread.sleep(500);
        }

        assertTrue(dltStore.containsKey(eventId));
        NotificationDeadLetter deadLetter = dltStore.get(eventId);
        assertNotNull(deadLetter);
        assertEquals(eventId, deadLetter.getEventId());
        assertTrue(deadLetter.getErrorMessage().contains("Artificial failure triggered"));
    }
}
