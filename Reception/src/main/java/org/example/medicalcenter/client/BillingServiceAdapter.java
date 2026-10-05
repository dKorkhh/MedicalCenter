package org.example.medicalcenter.client;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.example.medicalcenter.client.dto.InvoiceDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillingServiceAdapter {

    private final BillingClient billingClient;

    @Bulkhead(name = "billingClient")
    @CircuitBreaker(name = "billingClient", fallbackMethod = "getInvoiceFallback")
    @Retry(name = "billingClient")
    public InvoiceDto getInvoice(String invoiceId, long delay) {
        return billingClient.getInvoiceById(invoiceId, delay);
    }

    @Bulkhead(name = "billingClient")
    @CircuitBreaker(name = "billingClient", fallbackMethod = "getInvoicesBatchFallback")
    @Retry(name = "billingClient")
    public List<InvoiceDto> getInvoicesBatch(List<String> ids) {
        return billingClient.getInvoicesBatch(ids);
    }

    public InvoiceDto getInvoiceFallback(String invoiceId, long delay, Throwable ex) {
        return InvoiceDto.builder()
                .id(invoiceId)
                .amount(BigDecimal.ZERO)
                .currency("UAH")
                .paymentStatus("UNAVAILABLE_TEMPORARILY")
                .build();
    }

    public List<InvoiceDto> getInvoicesBatchFallback(List<String> ids, Throwable ex) {
        return ids.stream()
                .map(id -> InvoiceDto.builder()
                        .id(id)
                        .amount(BigDecimal.ZERO)
                        .currency("UAH")
                        .paymentStatus("UNAVAILABLE_TEMPORARILY")
                        .build())
                .toList();
    }
}
