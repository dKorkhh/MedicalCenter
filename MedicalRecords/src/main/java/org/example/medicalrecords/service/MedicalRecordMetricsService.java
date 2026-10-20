package org.example.medicalrecords.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class MedicalRecordMetricsService {
    private final Counter createdRecordsCounter;
    private final Timer recordCreationTimer;

    public MedicalRecordMetricsService(MeterRegistry registry) {
        this.createdRecordsCounter = Counter.builder("medical.records.created.total")
                .description("Кількість створених медичних записів")
                .register(registry);
        this.recordCreationTimer = Timer.builder("medical.records.creation.duration")
                .description("Час створення медичного запису")
                .publishPercentileHistogram()
                .register(registry);
    }

    public Counter createdRecordsCounter() {
        return createdRecordsCounter;
    }

    public Timer recordCreationTimer() {
        return recordCreationTimer;
    }
}
