package org.example.billing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.billing.dto.RefundResponse;
import org.example.billing.event.AppointmentCancelledEvent;
import org.example.billing.mapper.RefundMapper;
import org.example.billing.model.Invoice;
import org.example.billing.model.Refund;
import org.example.billing.repository.InvoiceRepository;
import org.example.billing.repository.RefundRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private static final String REFUNDED_STATUS = "REFUNDED";
    private static final String PENDING_STATUS = "PENDING";
    private static final String DEFAULT_CURRENCY = "UAH";

    private final RefundRepository refundRepository;
    private final InvoiceRepository invoiceRepository;
    private final RefundMapper refundMapper;
    private final InvoiceMetricsService metricsService;

    @Override
    public RefundResponse processAppointmentCancellationRefund(AppointmentCancelledEvent event) {
        Optional<Refund> existingRefund = findExistingRefundForEvent(event);
        if (existingRefund.isPresent()) {
            log.info("Refund already processed for appointmentId={}, eventId={}", event.appointmentId(), event.eventId());
            return refundMapper.toResponse(existingRefund.get());
        }

        Optional<Invoice> targetInvoice = resolveTargetInvoice(event);
        targetInvoice.ifPresent(this::markInvoiceRefunded);

        String resolvedInvoiceId = targetInvoice.map(Invoice::getId).orElse(event.invoiceId());
        BigDecimal resolvedAmount = resolveRefundAmount(event, targetInvoice.orElse(null));
        String resolvedCurrency = resolveRefundCurrency(event, targetInvoice.orElse(null));

        Refund savedRefund = refundRepository.save(
                refundMapper.toEntity(event, resolvedInvoiceId, resolvedAmount, resolvedCurrency));
        return refundMapper.toResponse(savedRefund);
    }

    @Override
    public RefundResponse getRefundById(String id) {
        return refundRepository.findById(id)
                .map(refundMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Refund not found with id: " + id));
    }

    @Override
    public List<RefundResponse> getAllRefunds() {
        return refundMapper.toResponseList(refundRepository.findAll());
    }

    @Override
    public List<RefundResponse> getRefundsByPatientId(String patientId) {
        return refundMapper.toResponseList(refundRepository.findByPatientId(patientId));
    }

    private Optional<Refund> findExistingRefundForEvent(AppointmentCancelledEvent event) {
        if (event.appointmentId() != null) {
            Optional<Refund> byAppointment = refundRepository.findByAppointmentId(event.appointmentId());
            if (byAppointment.isPresent()) {
                return byAppointment;
            }
        }
        return Optional.empty();
    }

    private Optional<Invoice> resolveTargetInvoice(AppointmentCancelledEvent event) {
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

    private void markInvoiceRefunded(Invoice invoice) {
        if (REFUNDED_STATUS.equalsIgnoreCase(invoice.getPaymentStatus())) {
            return;
        }
        boolean wasPending = PENDING_STATUS.equalsIgnoreCase(invoice.getPaymentStatus());
        invoice.setPaymentStatus(REFUNDED_STATUS);
        invoiceRepository.save(invoice);
        if (wasPending && metricsService != null) {
            metricsService.pending().decrementAndGet();
        }
    }

    private BigDecimal resolveRefundAmount(AppointmentCancelledEvent event, Invoice invoice) {
        if (event.refundAmount() != null && event.refundAmount().compareTo(BigDecimal.ZERO) > 0) {
            return event.refundAmount();
        }
        if (invoice != null && invoice.getAmount() != null) {
            return invoice.getAmount();
        }
        return BigDecimal.ZERO;
    }

    private String resolveRefundCurrency(AppointmentCancelledEvent event, Invoice invoice) {
        if (event.currency() != null && !event.currency().isBlank()) {
            return event.currency();
        }
        if (invoice != null && invoice.getCurrency() != null && !invoice.getCurrency().isBlank()) {
            return invoice.getCurrency();
        }
        return DEFAULT_CURRENCY;
    }
}
