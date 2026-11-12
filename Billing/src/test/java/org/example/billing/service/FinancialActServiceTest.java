package org.example.billing.service;

import org.example.billing.dto.FinancialActResponse;
import org.example.billing.dto.PaymentSnapshotResponse;
import org.example.billing.event.AppointmentCompletedEvent;
import org.example.billing.mapper.FinancialActMapper;
import org.example.billing.mapper.FinancialActMapperImpl;
import org.example.billing.model.FinancialAct;
import org.example.billing.model.Invoice;
import org.example.billing.model.PaymentSnapshot;
import org.example.billing.repository.FinancialActRepository;
import org.example.billing.repository.InvoiceRepository;
import org.example.billing.repository.PaymentSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialActServiceTest {

    @Mock
    private FinancialActRepository financialActRepository;

    @Mock
    private PaymentSnapshotRepository paymentSnapshotRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceMetricsService metricsService;

    @Spy
    private FinancialActMapper financialActMapper = new FinancialActMapperImpl();

    private FinancialActServiceImpl financialActService;

    @BeforeEach
    void setUp() {
        financialActService = new FinancialActServiceImpl(
                financialActRepository,
                paymentSnapshotRepository,
                invoiceRepository,
                financialActMapper,
                metricsService
        );
    }

    @Test
    void shouldGenerateFinancialActAndClosePaymentSnapshotWhenAppointmentCompleted() {
        LocalDateTime now = LocalDateTime.now();
        AppointmentCompletedEvent event = new AppointmentCompletedEvent(
                "evt-200",
                "APPOINTMENT_COMPLETED",
                "appt-200",
                "slot-200",
                "pat-200",
                "Taras Shevchenko",
                "taras@example.com",
                "doc-200",
                "Dr. Franko",
                now.minusHours(1),
                "204",
                "Cardiology Consultation",
                new BigDecimal("750.00"),
                "UAH",
                "inv-200",
                "Hypertension Stage 1",
                "Lisinopril 10mg",
                "Regular BP check",
                now,
                now
        );

        Invoice existingInvoice = Invoice.builder()
                .id("inv-200")
                .appointmentId("appt-200")
                .patientId("pat-200")
                .amount(new BigDecimal("750.00"))
                .currency("UAH")
                .paymentStatus("PENDING")
                .issuedAt(now.minusHours(2))
                .build();

        when(financialActRepository.findByAppointmentId("appt-200")).thenReturn(Optional.empty());
        when(invoiceRepository.findById("inv-200")).thenReturn(Optional.of(existingInvoice));
        when(metricsService.pending()).thenReturn(new AtomicInteger(1));

        when(financialActRepository.save(any(FinancialAct.class))).thenAnswer(inv -> {
            FinancialAct a = inv.getArgument(0);
            a.setId("act-generated-id");
            return a;
        });
        when(paymentSnapshotRepository.save(any(PaymentSnapshot.class))).thenAnswer(inv -> {
            PaymentSnapshot s = inv.getArgument(0);
            s.setId("snap-generated-id");
            return s;
        });

        FinancialActResponse response = financialActService.processAppointmentCompletion(event);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("act-generated-id");
        assertThat(response.appointmentId()).isEqualTo("appt-200");
        assertThat(response.patientId()).isEqualTo("pat-200");
        assertThat(response.doctorId()).isEqualTo("doc-200");
        assertThat(response.serviceName()).isEqualTo("Cardiology Consultation");
        assertThat(response.amount()).isEqualByComparingTo(new BigDecimal("750.00"));
        assertThat(response.status()).isEqualTo("SETTLED");

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(invoiceCaptor.capture());
        assertThat(invoiceCaptor.getValue().getPaymentStatus()).isEqualTo("PAID");
        assertThat(invoiceCaptor.getValue().getPaidAt()).isEqualTo(now);

        ArgumentCaptor<PaymentSnapshot> snapshotCaptor = ArgumentCaptor.forClass(PaymentSnapshot.class);
        verify(paymentSnapshotRepository).save(snapshotCaptor.capture());
        assertThat(snapshotCaptor.getValue().getSnapshotStatus()).isEqualTo("CLOSED");
        assertThat(snapshotCaptor.getValue().getInitialPaymentStatus()).isEqualTo("PENDING");
        assertThat(snapshotCaptor.getValue().getFinalPaymentStatus()).isEqualTo("PAID");
        assertThat(snapshotCaptor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("750.00"));

        verify(financialActRepository, times(1)).save(any(FinancialAct.class));
    }

    @Test
    void shouldBeIdempotentWhenProcessingDuplicateAppointmentCompletedEvent() {
        AppointmentCompletedEvent event = new AppointmentCompletedEvent(
                "evt-200",
                "APPOINTMENT_COMPLETED",
                "appt-200",
                "slot-200",
                "pat-200",
                "Taras Shevchenko",
                "taras@example.com",
                "doc-200",
                "Dr. Franko",
                LocalDateTime.now(),
                "204",
                "Cardiology Consultation",
                new BigDecimal("750.00"),
                "UAH",
                "inv-200",
                "Hypertension Stage 1",
                "Lisinopril 10mg",
                "Regular BP check",
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        FinancialAct existing = FinancialAct.builder()
                .id("act-existing")
                .actNumber("ACT-appt-200")
                .appointmentId("appt-200")
                .patientId("pat-200")
                .doctorId("doc-200")
                .status("SETTLED")
                .build();

        when(financialActRepository.findByAppointmentId("appt-200")).thenReturn(Optional.of(existing));

        FinancialActResponse response = financialActService.processAppointmentCompletion(event);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("act-existing");
        verify(financialActRepository, never()).save(any(FinancialAct.class));
        verify(paymentSnapshotRepository, never()).save(any(PaymentSnapshot.class));
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void shouldThrowNotFoundWhenFinancialActDoesNotExist() {
        when(financialActRepository.findById("missing-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> financialActService.getFinancialActById("missing-id"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Financial act not found with id: missing-id");
    }

    @Test
    void shouldReturnPaymentSnapshotByAppointmentId() {
        PaymentSnapshot snapshot = PaymentSnapshot.builder()
                .id("snap-1")
                .appointmentId("appt-200")
                .invoiceId("inv-200")
                .patientId("pat-200")
                .amount(new BigDecimal("750.00"))
                .currency("UAH")
                .snapshotStatus("CLOSED")
                .initialPaymentStatus("PENDING")
                .finalPaymentStatus("PAID")
                .closedAt(LocalDateTime.now())
                .build();

        when(paymentSnapshotRepository.findByAppointmentId("appt-200")).thenReturn(Optional.of(snapshot));

        PaymentSnapshotResponse response = financialActService.getPaymentSnapshotByAppointmentId("appt-200");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("snap-1");
        assertThat(response.snapshotStatus()).isEqualTo("CLOSED");
        assertThat(response.finalPaymentStatus()).isEqualTo("PAID");
    }

    @Test
    void shouldReturnFinancialActsByPatientId() {
        FinancialAct act1 = FinancialAct.builder().id("a1").patientId("pat-1").status("SETTLED").build();
        FinancialAct act2 = FinancialAct.builder().id("a2").patientId("pat-1").status("SETTLED").build();

        when(financialActRepository.findByPatientId("pat-1")).thenReturn(List.of(act1, act2));

        List<FinancialActResponse> results = financialActService.getFinancialActsByPatientId("pat-1");

        assertThat(results).hasSize(2);
        assertThat(results.get(0).id()).isEqualTo("a1");
        assertThat(results.get(1).id()).isEqualTo("a2");
    }
}
