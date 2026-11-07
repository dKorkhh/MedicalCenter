package org.example.billing.service;

import lombok.RequiredArgsConstructor;
import org.example.billing.dto.InvoiceResponse;
import org.example.billing.security.CurrentUserProvider;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceAccessService {

    private final InvoiceService invoiceService;
    private final CurrentUserProvider currentUserProvider;

    public InvoiceResponse getInvoiceVisibleToCurrentUser(String id) {
        InvoiceResponse invoice = invoiceService.getInvoiceById(id);
        verifyCurrentUserCanViewInvoice(invoice);
        return invoice;
    }

    public List<InvoiceResponse> getInvoicesVisibleToCurrentUser(String requestedPatientId) {
        if (!currentUserProvider.isAdmin()) {
            return invoiceService.getInvoicesByPatientId(currentUserProvider.currentUserId());
        }
        if (requestedPatientId == null || requestedPatientId.isBlank()) {
            return invoiceService.getAllInvoices();
        }
        return invoiceService.getInvoicesByPatientId(requestedPatientId);
    }

    public List<InvoiceResponse> getInvoicesBatchVisibleToCurrentUser(List<String> ids) {
        return invoiceService.getInvoicesByIds(ids).stream()
                .filter(this::isInvoiceVisibleToCurrentUser)
                .toList();
    }

    private void verifyCurrentUserCanViewInvoice(InvoiceResponse invoice) {
        if (!isInvoiceVisibleToCurrentUser(invoice)) {
            throw new AccessDeniedException("Invoice " + invoice.id() + " does not belong to the current user");
        }
    }

    private boolean isInvoiceVisibleToCurrentUser(InvoiceResponse invoice) {
        return currentUserProvider.isAdmin() || currentUserProvider.currentUserId().equals(invoice.patientId());
    }
}
