package org.example.billing.service;

import lombok.RequiredArgsConstructor;
import org.example.billing.dto.CreateInvoiceRequest;
import org.example.billing.dto.InvoiceResponse;
import org.example.billing.model.Invoice;
import org.example.billing.repository.InvoiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;

    @Override
    public InvoiceResponse issueInvoice(CreateInvoiceRequest request) {
        Invoice invoice = Invoice.builder()
                .appointmentId(request.appointmentId())
                .patientId(request.patientId())
                .amount(request.amount())
                .currency(request.currency())
                .paymentStatus("PENDING")
                .issuedAt(LocalDateTime.now())
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);
        return mapToResponse(savedInvoice);
    }

    @Override
    public InvoiceResponse getInvoiceById(String id) {
        return invoiceRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found with id: " + id));
    }

    @Override
    public List<InvoiceResponse> getAllInvoices() {
        return invoiceRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<InvoiceResponse> getInvoicesByPatientId(String patientId) {
        return invoiceRepository.findByPatientId(patientId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private InvoiceResponse mapToResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getAppointmentId(),
                invoice.getPatientId(),
                invoice.getAmount(),
                invoice.getCurrency(),
                invoice.getPaymentStatus(),
                invoice.getIssuedAt(),
                invoice.getPaidAt()
        );
    }
}
