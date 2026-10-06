package org.example.reception.client;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.example.reception.client.dto.InvoiceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@Service
public class BillingServiceAdapter {

    private static final Logger log = LoggerFactory.getLogger(BillingServiceAdapter.class);

    public static final int MAX_BATCH_SIZE = 100;
    public static final String UNAVAILABLE_STATUS = "UNAVAILABLE_TEMPORARILY";

    private final BillingClient billingClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final Bulkhead bulkhead;

    public BillingServiceAdapter(BillingClient billingClient, CircuitBreaker billingCircuitBreaker,
                                 Retry billingRetry, Bulkhead billingBulkhead) {
        this.billingClient = billingClient;
        this.circuitBreaker = billingCircuitBreaker;
        this.retry = billingRetry;
        this.bulkhead = billingBulkhead;
    }

    public InvoiceDto getInvoice(String invoiceId, long delay) {
        return execute("getInvoice",
                () -> billingClient.getInvoiceById(invoiceId, delay),
                () -> fallbackInvoice(invoiceId));
    }

    public List<InvoiceDto> getInvoicesBatch(List<String> ids) {
        List<String> distinct = ids == null ? List.of()
                : ids.stream().filter(Objects::nonNull).distinct().toList();
        List<InvoiceDto> result = new ArrayList<>();
        for (int from = 0; from < distinct.size(); from += MAX_BATCH_SIZE) {
            List<String> chunk = List.copyOf(distinct.subList(from, Math.min(from + MAX_BATCH_SIZE, distinct.size())));
            result.addAll(execute("getInvoicesBatch",
                    () -> billingClient.getInvoicesBatch(chunk),
                    () -> chunk.stream().map(this::fallbackInvoice).toList()));
        }
        return result;
    }

    private <T> T execute(String operation, Supplier<T> call, Supplier<T> fallback) {
        Supplier<T> decorated = Retry.decorateSupplier(retry,
                CircuitBreaker.decorateSupplier(circuitBreaker,
                        Bulkhead.decorateSupplier(bulkhead, call)));
        try {
            return decorated.get();
        } catch (HttpClientErrorException e) {
            throw toDownstreamProblem(e);
        } catch (CallNotPermittedException | BulkheadFullException | RestClientException e) {
            log.warn("[Billing:{}] using fallback, reason: {}", operation, e.toString());
            return fallback.get();
        }
    }

    private DownstreamProblemException toDownstreamProblem(RestClientResponseException e) {
        DownstreamProblem problem = null;
        try {
            problem = e.getResponseBodyAs(DownstreamProblem.class);
        } catch (RuntimeException parseFailure) {
            log.debug("Cannot parse downstream problem body: {}", parseFailure.toString());
        }
        return new DownstreamProblemException(e.getStatusCode().value(), problem, e);
    }

    private InvoiceDto fallbackInvoice(String invoiceId) {
        return InvoiceDto.builder()
                .id(invoiceId)
                .amount(BigDecimal.ZERO)
                .currency("UAH")
                .paymentStatus(UNAVAILABLE_STATUS)
                .build();
    }
}
