package org.example.reception.controller;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/resilience")
@RequiredArgsConstructor
public class ResilienceStatusController {

    private final CircuitBreaker billingCircuitBreaker;
    private final Bulkhead billingBulkhead;

    @GetMapping("/billing")
    public Map<String, Object> billingStatus() {
        CircuitBreaker.Metrics cb = billingCircuitBreaker.getMetrics();
        Map<String, Object> circuit = new LinkedHashMap<>();
        circuit.put("state", billingCircuitBreaker.getState().name());
        circuit.put("failureRate", cb.getFailureRate());
        circuit.put("bufferedCalls", cb.getNumberOfBufferedCalls());
        circuit.put("failedCalls", cb.getNumberOfFailedCalls());
        circuit.put("notPermittedCalls", cb.getNumberOfNotPermittedCalls());

        Map<String, Object> bulkhead = new LinkedHashMap<>();
        bulkhead.put("availableConcurrentCalls", billingBulkhead.getMetrics().getAvailableConcurrentCalls());
        bulkhead.put("maxAllowedConcurrentCalls", billingBulkhead.getMetrics().getMaxAllowedConcurrentCalls());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("circuitBreaker", circuit);
        result.put("bulkhead", bulkhead);
        return result;
    }
}
