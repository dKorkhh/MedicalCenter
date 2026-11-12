package org.example.medicalrecords.service;

import org.example.medicalrecords.dto.VisitRecordResponse;
import org.example.medicalrecords.event.AppointmentCompletedEvent;
import org.example.medicalrecords.model.MedicalRecord;
import org.example.medicalrecords.model.VisitRecord;
import org.example.medicalrecords.repository.MedicalRecordRepository;
import org.example.medicalrecords.repository.VisitRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitRecordServiceTest {

    @Mock
    private VisitRecordRepository visitRecordRepository;

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    private VisitRecordServiceImpl visitRecordService;

    @BeforeEach
    void setUp() {
        visitRecordService = new VisitRecordServiceImpl(visitRecordRepository, medicalRecordRepository);
    }

    @Test
    void shouldCreateVisitRecordAndSyncMedicalRecordWhenEventReceived() {
        AppointmentCompletedEvent event = new AppointmentCompletedEvent(
                "evt-100",
                "APPOINTMENT_COMPLETED",
                "appt-100",
                "slot-100",
                "pat-100",
                "Olena Koval",
                "olena@example.com",
                "doc-100",
                "Dr. Shevchenko",
                LocalDateTime.now(),
                "305",
                "General Consultation",
                new BigDecimal("500.00"),
                "UAH",
                "inv-100",
                "Acute Bronchitis",
                "Amoxicillin 500mg",
                "Drink plenty of water and rest",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(visitRecordRepository.findByAppointmentId("appt-100")).thenReturn(Optional.empty());
        when(visitRecordRepository.save(any(VisitRecord.class))).thenAnswer(inv -> {
            VisitRecord r = inv.getArgument(0);
            r.setId("vr-generated-id");
            return r;
        });
        when(medicalRecordRepository.existsByAppointmentId("appt-100")).thenReturn(false);

        VisitRecord result = visitRecordService.processAppointmentCompleted(event);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("vr-generated-id");
        assertThat(result.getAppointmentId()).isEqualTo("appt-100");
        assertThat(result.getDiagnosis()).isEqualTo("Acute Bronchitis");
        assertThat(result.getPrescription()).isEqualTo("Amoxicillin 500mg");

        verify(visitRecordRepository, times(1)).save(any(VisitRecord.class));

        ArgumentCaptor<MedicalRecord> medCaptor = ArgumentCaptor.forClass(MedicalRecord.class);
        verify(medicalRecordRepository, times(1)).save(medCaptor.capture());
        assertThat(medCaptor.getValue().getAppointmentId()).isEqualTo("appt-100");
        assertThat(medCaptor.getValue().getDiagnosis()).isEqualTo("Acute Bronchitis");
    }

    @Test
    void shouldBeIdempotentWhenProcessingDuplicateEvent() {
        AppointmentCompletedEvent event = new AppointmentCompletedEvent(
                "evt-100",
                "APPOINTMENT_COMPLETED",
                "appt-100",
                "slot-100",
                "pat-100",
                "Olena Koval",
                "olena@example.com",
                "doc-100",
                "Dr. Shevchenko",
                LocalDateTime.now(),
                "305",
                "General Consultation",
                new BigDecimal("500.00"),
                "UAH",
                "inv-100",
                "Acute Bronchitis",
                "Amoxicillin 500mg",
                "Rest",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        VisitRecord existing = VisitRecord.builder()
                .id("vr-existing")
                .appointmentId("appt-100")
                .build();

        when(visitRecordRepository.findByAppointmentId("appt-100")).thenReturn(Optional.of(existing));

        VisitRecord result = visitRecordService.processAppointmentCompleted(event);

        assertThat(result).isSameAs(existing);
        verify(visitRecordRepository, never()).save(any(VisitRecord.class));
        verify(medicalRecordRepository, never()).save(any(MedicalRecord.class));
    }

    @Test
    void shouldReturnVisitRecordByAppointmentId() {
        VisitRecord existing = VisitRecord.builder()
                .id("vr-1")
                .appointmentId("appt-100")
                .patientId("pat-100")
                .doctorId("doc-100")
                .diagnosis("Flu")
                .build();

        when(visitRecordRepository.findByAppointmentId("appt-100")).thenReturn(Optional.of(existing));

        VisitRecordResponse response = visitRecordService.getVisitRecordByAppointmentId("appt-100");

        assertThat(response).isNotNull();
        assertThat(response.appointmentId()).isEqualTo("appt-100");
        assertThat(response.diagnosis()).isEqualTo("Flu");
    }

    @Test
    void shouldReturnVisitRecordsByPatientId() {
        VisitRecord r1 = VisitRecord.builder().id("vr-1").patientId("pat-1").diagnosis("D1").build();
        VisitRecord r2 = VisitRecord.builder().id("vr-2").patientId("pat-1").diagnosis("D2").build();

        when(visitRecordRepository.findByPatientIdOrderByCompletedAtDesc("pat-1")).thenReturn(List.of(r1, r2));

        List<VisitRecordResponse> responses = visitRecordService.getVisitRecordsByPatientId("pat-1");

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).diagnosis()).isEqualTo("D1");
        assertThat(responses.get(1).diagnosis()).isEqualTo("D2");
    }
}
