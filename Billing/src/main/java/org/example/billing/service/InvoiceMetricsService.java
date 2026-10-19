package org.example.billing.service;

import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.*;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class InvoiceMetricsService {
    private final Timer processing;
    private final DistributionSummary amount;
    private final AtomicInteger pending = new AtomicInteger();

    public InvoiceMetricsService(MeterRegistry registry) {
        processing = Timer.builder("billing.invoice.processing")
                .publishPercentileHistogram()
                .register(registry);

        amount = DistributionSummary.builder("billing.invoice.amount")
                .baseUnit("uah")
                .publishPercentileHistogram()
                .register(registry);

        Gauge.builder("billing.invoices.pending", pending, AtomicInteger::get)
                .register(registry);
    }

    public Timer processing() {
        return processing;
    }

    public DistributionSummary amount() {
        return amount;
    }

    public AtomicInteger pending() {
        return pending;
    }
}
