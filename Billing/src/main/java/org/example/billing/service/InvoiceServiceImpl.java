package org.example.billing.service;

import lombok.RequiredArgsConstructor;
import org.example.billing.dto.CreateInvoiceRequest;
import org.example.billing.exception.ApiProblemException;
import org.example.billing.dto.InvoiceResponse;
import org.example.billing.mapper.InvoiceMapper;
import org.example.billing.model.Invoice;
import org.example.billing.repository.InvoiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    public static final int MAX_BATCH_SIZE = 100;

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final InvoiceMetricsService metricsService;

    @Override
    public InvoiceResponse issueInvoice(CreateInvoiceRequest request) {
        return metricsService.processing().record(() -> {
            Invoice invoice = invoiceMapper.toEntity(request);
            Invoice savedInvoice = invoiceRepository.save(invoice);
            if (savedInvoice.getAmount() != null) {
                metricsService.amount().record(savedInvoice.getAmount().doubleValue());
            }
            if ("PENDING".equalsIgnoreCase(savedInvoice.getPaymentStatus())) {
                metricsService.pending().incrementAndGet();
            }
            return invoiceMapper.toResponse(savedInvoice);
        });
    }

    @Override
    public InvoiceResponse getInvoiceById(String id) {
        return invoiceRepository.findById(id)
                .map(invoiceMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found with id: " + id));
    }

    @Override
    public List<InvoiceResponse> getAllInvoices() {
        return invoiceRepository.findAll()
                .stream()
                .map(invoiceMapper::toResponse)
                .toList();
    }

    @Override
    public List<InvoiceResponse> getInvoicesByPatientId(String patientId) {
        return invoiceRepository.findByPatientId(patientId)
                .stream()
                .map(invoiceMapper::toResponse)
                .toList();
    }

    @Override
    public List<InvoiceResponse> getInvoicesByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<String> distinctIds = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (distinctIds.size() > MAX_BATCH_SIZE) {
            throw new ApiProblemException(HttpStatus.BAD_REQUEST, "batch-too-large", "Batch too large",
                    "At most " + MAX_BATCH_SIZE + " ids are allowed per batch request, got " + distinctIds.size() + ".");
        }
        return invoiceRepository.findByIdIn(distinctIds).stream()
                .map(invoiceMapper::toResponse)
                .toList();
    }
}
