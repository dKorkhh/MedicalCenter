package org.example.billing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.billing.dto.FinancialActResponse;
import org.example.billing.dto.PaymentSnapshotResponse;
import org.example.billing.event.AppointmentCompletedEvent;
import org.example.billing.mapper.FinancialActMapper;
import org.example.billing.model.FinancialAct;
import org.example.billing.model.Invoice;
import org.example.billing.model.PaymentSnapshot;
import org.example.billing.repository.FinancialActRepository;
import org.example.billing.repository.InvoiceRepository;
import org.example.billing.repository.PaymentSnapshotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialActServiceImpl implements FinancialActService {

    private static final String DEFAULT_CURRENCY = "UAH";
    private static final String DEFAULT_SERVICE_NAME = "Medical Consultation";
    private static final String SETTLED_STATUS = "SETTLED";
    private static final String PAID_STATUS = "PAID";
    private static final String PENDING_STATUS = "PENDING";
    private static final String CLOSED_STATUS = "CLOSED";

    private final FinancialActRepository financialActRepository;
    private final PaymentSnapshotRepository paymentSnapshotRepository;
    private final InvoiceRepository invoiceRepository;
    private final FinancialActMapper financialActMapper;
    private final InvoiceMetricsService metricsService;

    @Override
    public FinancialActResponse processAppointmentCompletion(AppointmentCompletedEvent event) {
        Optional<FinancialAct> existingAct = findExistingActForEvent(event);
        if (existingAct.isPresent()) {
            log.info("Financial act already processed for appointmentId={}, eventId={}",
                    event.appointmentId(), event.eventId());
            return financialActMapper.toResponse(existingAct.get());
        }

        Optional<Invoice> targetInvoice = resolveTargetInvoice(event);
        String initialStatus = targetInvoice.map(Invoice::getPaymentStatus).orElse(PENDING_STATUS);
        targetInvoice.ifPresent(inv -> settleInvoice(inv, event));

        String resolvedInvoiceId = targetInvoice.map(Invoice::getId).orElse(event.invoiceId());
        BigDecimal resolvedAmount = resolveActAmount(event, targetInvoice.orElse(null));
        String resolvedCurrency = resolveActCurrency(event, targetInvoice.orElse(null));
        LocalDateTime completionTime = event.completedAt() != null ? event.completedAt() : LocalDateTime.now();

        createAndSavePaymentSnapshot(event, resolvedInvoiceId, resolvedAmount, resolvedCurrency, initialStatus, completionTime);

        FinancialAct savedAct = createAndSaveFinancialAct(event, resolvedInvoiceId, resolvedAmount, resolvedCurrency, completionTime);
        return financialActMapper.toResponse(savedAct);
    }

    @Override
    public FinancialActResponse getFinancialActById(String id) {
        return financialActRepository.findById(id)
                .map(financialActMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Financial act not found with id: " + id));
    }

    @Override
    public FinancialActResponse getFinancialActByAppointmentId(String appointmentId) {
        return financialActRepository.findByAppointmentId(appointmentId)
                .map(financialActMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Financial act not found for appointment: " + appointmentId));
    }

    @Override
    public List<FinancialActResponse> getAllFinancialActs() {
        return financialActMapper.toResponseList(financialActRepository.findAll());
    }

    @Override
    public List<FinancialActResponse> getFinancialActsByPatientId(String patientId) {
        return financialActMapper.toResponseList(financialActRepository.findByPatientId(patientId));
    }

    @Override
    public List<FinancialActResponse> getFinancialActsByDoctorId(String doctorId) {
        return financialActMapper.toResponseList(financialActRepository.findByDoctorId(doctorId));
    }

    @Override
    public PaymentSnapshotResponse getPaymentSnapshotByAppointmentId(String appointmentId) {
        return paymentSnapshotRepository.findByAppointmentId(appointmentId)
                .map(financialActMapper::toSnapshotResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment snapshot not found for appointment: " + appointmentId));
    }

    private Optional<FinancialAct> findExistingActForEvent(AppointmentCompletedEvent event) {
        if (event.appointmentId() != null && !event.appointmentId().isBlank()) {
            return financialActRepository.findByAppointmentId(event.appointmentId());
        }
        return Optional.empty();
    }

    private Optional<Invoice> resolveTargetInvoice(AppointmentCompletedEvent event) {
        if (event.invoiceId() != null && !event.invoiceId().isBlank()) {
            Optional<Invoice> byId = invoiceRepository.findById(event.invoiceId());
            if (byId.isPresent()) {
                return byId;
            }
        }
        if (event.appointmentId() != null && !event.appointmentId().isBlank()) {
            return invoiceRepository.findByAppointmentId(event.appointmentId()).stream().findFirst();
        }
        return Optional.empty();
    }

    private void settleInvoice(Invoice invoice, AppointmentCompletedEvent event) {
        if (PAID_STATUS.equalsIgnoreCase(invoice.getPaymentStatus())) {
            return;
        }
        boolean wasPending = PENDING_STATUS.equalsIgnoreCase(invoice.getPaymentStatus());
        invoice.setPaymentStatus(PAID_STATUS);
        invoice.setPaidAt(event.completedAt() != null ? event.completedAt() : LocalDateTime.now());
        invoiceRepository.save(invoice);
        if (wasPending && metricsService != null) {
            metricsService.pending().decrementAndGet();
        }
    }

    private BigDecimal resolveActAmount(AppointmentCompletedEvent event, Invoice invoice) {
        if (event.amount() != null && event.amount().compareTo(BigDecimal.ZERO) > 0) {
            return event.amount();
        }
        if (invoice != null && invoice.getAmount() != null) {
            return invoice.getAmount();
        }
        return BigDecimal.ZERO;
    }

    private String resolveActCurrency(AppointmentCompletedEvent event, Invoice invoice) {
        if (event.currency() != null && !event.currency().isBlank()) {
            return event.currency();
        }
        if (invoice != null && invoice.getCurrency() != null && !invoice.getCurrency().isBlank()) {
            return invoice.getCurrency();
        }
        return DEFAULT_CURRENCY;
    }

    private PaymentSnapshot createAndSavePaymentSnapshot(AppointmentCompletedEvent event,
                                                        String invoiceId,
                                                        BigDecimal amount,
                                                        String currency,
                                                        String initialStatus,
                                                        LocalDateTime closedAt) {
        PaymentSnapshot snapshot = PaymentSnapshot.builder()
                .appointmentId(event.appointmentId())
                .invoiceId(invoiceId)
                .patientId(event.patientId())
                .amount(amount)
                .currency(currency)
                .snapshotStatus(CLOSED_STATUS)
                .initialPaymentStatus(initialStatus)
                .finalPaymentStatus(PAID_STATUS)
                .closedAt(closedAt)
                .build();
        return paymentSnapshotRepository.save(snapshot);
    }

    private FinancialAct createAndSaveFinancialAct(AppointmentCompletedEvent event,
                                                  String invoiceId,
                                                  BigDecimal amount,
                                                  String currency,
                                                  LocalDateTime closedAt) {
        String actNumber = "ACT-" + (event.appointmentId() != null ? event.appointmentId() : UUID.randomUUID().toString());
        String resolvedServiceName = (event.serviceName() != null && !event.serviceName().isBlank())
                ? event.serviceName()
                : DEFAULT_SERVICE_NAME;
        LocalDateTime issuedAt = event.appointmentDateTime() != null ? event.appointmentDateTime() : closedAt;

        FinancialAct act = FinancialAct.builder()
                .actNumber(actNumber)
                .appointmentId(event.appointmentId())
                .patientId(event.patientId())
                .patientFullName(event.patientFullName())
                .doctorId(event.doctorId())
                .doctorFullName(event.doctorFullName())
                .invoiceId(invoiceId)
                .serviceName(resolvedServiceName)
                .amount(amount)
                .currency(currency)
                .status(SETTLED_STATUS)
                .issuedAt(issuedAt)
                .closedAt(closedAt)
                .build();
        return financialActRepository.save(act);
    }
}
