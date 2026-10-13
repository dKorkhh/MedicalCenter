package org.example.reception.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.reception.client.BillingServiceAdapter;
import org.example.reception.dto.CreateAppointmentRequest;
import org.example.reception.model.Appointment;
import org.example.reception.repository.AppointmentRepository;
import org.example.reception.service.AppointmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionalOutboxIntegrationTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private BillingServiceAdapter billingServiceAdapter;

    @Mock
    private OutboxMessageRepository outboxMessageRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxEventPublisher outboxEventPublisher;

    @BeforeEach
    void setUp() {
        outboxEventPublisher = new OutboxEventPublisher(outboxMessageRepository, kafkaTemplate);
    }

    @Test
    void testScheduleAppointment_atomicSaveOfEntityAndOutbox() {
        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .patientId("123")
                .patientFullName("Test user")
                .patientEmail("test@gmai.com")
                .doctorId("456")
                .doctorFullName("Test doc")
                .appointmentDateTime(LocalDateTime.now().plusDays(2))
                .roomNumber("101")
                .invoiceId("999")
                .build();

        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment app = invocation.getArgument(0);
            app.setId("apt-generated-id");
            return app;
        });

        when(outboxMessageRepository.save(any(OutboxMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = appointmentService.scheduleAppointment(request);

        assertNotNull(response);
        assertEquals("apt-generated-id", response.id());

        verify(appointmentRepository, times(1)).save(any(Appointment.class));

        ArgumentCaptor<OutboxMessage> outboxCaptor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxMessageRepository, times(1)).save(outboxCaptor.capture());

        OutboxMessage capturedMessage = outboxCaptor.getValue();
        assertEquals("apt-generated-id", capturedMessage.getAggregateId());
        assertEquals("Appointment", capturedMessage.getAggregateType());
        assertEquals("APPOINTMENT_BOOKED", capturedMessage.getEventType());
        assertEquals(OutboxStatus.PENDING, capturedMessage.getStatus());
        assertNotNull(capturedMessage.getPayload());
        assertTrue(capturedMessage.getPayload().contains("123"));
        assertTrue(capturedMessage.getPayload().contains("APPOINTMENT_BOOKED"));

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void testDualWriteElimination_whenEntityFails_outboxNotSaved() {
        CreateAppointmentRequest request = CreateAppointmentRequest.builder()
                .patientId("123")
                .doctorId("456")
                .appointmentDateTime(LocalDateTime.now().plusDays(2))
                .build();

        when(appointmentRepository.save(any(Appointment.class)))
                .thenThrow(new RuntimeException("Database error: simulated connection abort"));

        assertThrows(RuntimeException.class, () -> appointmentService.scheduleAppointment(request));

        verify(outboxMessageRepository, never()).save(any(OutboxMessage.class));
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void testOutboxPublisher_publishesPendingEventsAndUpdatesStatus() {
        OutboxMessage pendingMessage = OutboxMessage.builder()
                .id(UUID.randomUUID().toString())
                .aggregateId("1019")
                .aggregateType("Appointment")
                .eventType("APPOINTMENT_BOOKED")
                .payload("{\"appointmentId\":\"1019\",\"patientId\":\"123\"}")
                .status(OutboxStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .build();

        when(outboxMessageRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(pendingMessage));

        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        when(outboxMessageRepository.save(any(OutboxMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        outboxEventPublisher.publishPendingEvents();

        verify(kafkaTemplate, times(1)).send(anyString(), eq("1019"), eq(pendingMessage.getPayload()));

        verify(outboxMessageRepository, times(1)).save(pendingMessage);
        assertEquals(OutboxStatus.PROCESSED, pendingMessage.getStatus());
        assertNotNull(pendingMessage.getProcessedAt());
    }
}
