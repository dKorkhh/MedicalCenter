package org.example.reception.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class AppointmentMetricsService {
    private final Counter successfulAppointmentCounter;

    public AppointmentMetricsService(MeterRegistry registry) {
        this.successfulAppointmentCounter = Counter.builder("appointment.placed.total")
                .description("Кількість успішно створених записів")
                .tag("channel", "web")
                .register(registry);
    }

    public void recordAppointment() {
        successfulAppointmentCounter.increment();
    }
}
