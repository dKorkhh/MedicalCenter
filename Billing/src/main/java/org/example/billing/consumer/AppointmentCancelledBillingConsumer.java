package org.example.billing.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.billing.event.AppointmentCancelledEvent;
import org.example.billing.service.RefundService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentCancelledBillingConsumer {

    private final RefundService refundService;

    @KafkaListener(
            topics = "${app.kafka.topics.appointment-cancelled:medical.appointment.cancelled}",
            groupId = "${spring.kafka.consumer.group-id:billing-service-group}",
            containerFactory = "billingCancelledAppointmentKafkaListenerContainerFactory"
    )
    public void consumeAppointmentCancelledEvent(AppointmentCancelledEvent event) {
        log.info("Received AppointmentCancelledEvent in Billing: eventId={}, appointmentId={}",
                event.eventId(), event.appointmentId());
        refundService.processAppointmentCancellationRefund(event);
    }
}
