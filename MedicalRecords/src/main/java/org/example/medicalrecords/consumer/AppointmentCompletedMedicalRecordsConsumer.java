package org.example.medicalrecords.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.medicalrecords.event.AppointmentCompletedEvent;
import org.example.medicalrecords.service.VisitRecordService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentCompletedMedicalRecordsConsumer {

    private final VisitRecordService visitRecordService;

    @KafkaListener(
            topics = "${app.kafka.topics.appointment-completed:medical.appointment.completed}",
            groupId = "${spring.kafka.consumer.group-id:medical-records-service-group}",
            containerFactory = "medicalRecordsCompletedAppointmentKafkaListenerContainerFactory"
    )
    public void consumeAppointmentCompletedEvent(AppointmentCompletedEvent event) {
        log.info("Received AppointmentCompletedEvent in MedicalRecords: eventId={}, appointmentId={}, patientId={}",
                event.eventId(), event.appointmentId(), event.patientId());
        visitRecordService.processAppointmentCompleted(event);
    }
}
