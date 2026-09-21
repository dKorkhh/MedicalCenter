package org.example.billing.service;

import org.example.billing.dto.CreateInvoiceRequest;
import org.example.billing.dto.InvoiceResponse;

import java.util.List;

public interface InvoiceService {

    InvoiceResponse issueInvoice(CreateInvoiceRequest request);

    InvoiceResponse getInvoiceById(String id);

    List<InvoiceResponse> getAllInvoices();

    List<InvoiceResponse> getInvoicesByPatientId(String patientId);
}
