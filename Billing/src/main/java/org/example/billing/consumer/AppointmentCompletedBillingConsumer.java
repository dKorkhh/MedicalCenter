package org.example.billing.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.billing.event.AppointmentCompletedEvent;
import org.example.billing.service.FinancialActService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentCompletedBillingConsumer {

    private final FinancialActService financialActService;

    @KafkaListener(
            topics = "${app.kafka.topics.appointment-completed:medical.appointment.completed}",
            groupId = "${spring.kafka.consumer.group-id:billing-service-group}",
            containerFactory = "billingCompletedAppointmentKafkaListenerContainerFactory"
    )
    public void consumeAppointmentCompletedEvent(AppointmentCompletedEvent event) {
        log.info("Received AppointmentCompletedEvent in Billing: eventId={}, appointmentId={}",
                event.eventId(), event.appointmentId());
        financialActService.processAppointmentCompletion(event);
    }
}
