package org.example.notification.service;

import io.micrometer.core.instrument.LongTaskTimer;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class SendingMetricsService {
    private final LongTaskTimer reportTimer;

    public SendingMetricsService(MeterRegistry registry) {
        reportTimer = LongTaskTimer.builder("sending.email.processing")
                .register(registry);
    }

    public LongTaskTimer reportTimer() {
        return reportTimer;
    }
}
