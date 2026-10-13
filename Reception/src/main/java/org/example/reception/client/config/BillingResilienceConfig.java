package org.example.reception.client.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;

@Configuration
public class BillingResilienceConfig {

    private static final Logger log = LoggerFactory.getLogger(BillingResilienceConfig.class);
    public static final String NAME = "billingClient";

    @Bean
    public CircuitBreaker billingCircuitBreaker(
            @Value("${billing.resilience.circuit-breaker.sliding-window-size:10}") int slidingWindowSize,
            @Value("${billing.resilience.circuit-breaker.minimum-number-of-calls:5}") int minimumNumberOfCalls,
            @Value("${billing.resilience.circuit-breaker.failure-rate-threshold:50}") float failureRateThreshold,
            @Value("${billing.resilience.circuit-breaker.slow-call-duration-ms:2000}") long slowCallDurationMs,
            @Value("${billing.resilience.circuit-breaker.slow-call-rate-threshold:100}") float slowCallRateThreshold,
            @Value("${billing.resilience.circuit-breaker.wait-in-open-state-ms:10000}") long waitInOpenStateMs,
            @Value("${billing.resilience.circuit-breaker.half-open-calls:3}") int halfOpenCalls) {

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .failureRateThreshold(failureRateThreshold)
                .slowCallDurationThreshold(Duration.ofMillis(slowCallDurationMs))
                .slowCallRateThreshold(slowCallRateThreshold)
                .waitDurationInOpenState(Duration.ofMillis(waitInOpenStateMs))
                .permittedNumberOfCallsInHalfOpenState(halfOpenCalls)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(HttpServerErrorException.class, ResourceAccessException.class)
                .ignoreExceptions(HttpClientErrorException.class, BulkheadFullException.class)
                .build();

        CircuitBreaker circuitBreaker = CircuitBreaker.of(NAME, config);
        circuitBreaker.getEventPublisher()
                .onStateTransition(e -> log.warn("[CircuitBreaker:{}] state transition {}", NAME, e.getStateTransition()))
                .onCallNotPermitted(e -> log.debug("[CircuitBreaker:{}] call not permitted (OPEN)", NAME));
        return circuitBreaker;
    }

    @Bean
    public Retry billingRetry(
            @Value("${billing.resilience.retry.max-attempts:3}") int maxAttempts,
            @Value("${billing.resilience.retry.wait-duration-ms:500}") long waitDurationMs,
            @Value("${billing.resilience.retry.multiplier:2.0}") double multiplier,
            @Value("${billing.resilience.retry.jitter-factor:0.5}") double jitterFactor) {

        IntervalFunction backoffWithJitter =
                IntervalFunction.ofExponentialRandomBackoff(waitDurationMs, multiplier, jitterFactor);

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .intervalFunction(backoffWithJitter)
                .retryExceptions(HttpServerErrorException.class, ResourceAccessException.class)
                .ignoreExceptions(HttpClientErrorException.class, CallNotPermittedException.class, BulkheadFullException.class)
                .build();

        Retry retry = Retry.of(NAME, config);
        retry.getEventPublisher().onRetry(e -> log.warn("[Retry:{}] attempt #{} after {} ms, cause: {}",
                NAME, e.getNumberOfRetryAttempts(), e.getWaitInterval().toMillis(),
                e.getLastThrowable() == null ? "n/a" : e.getLastThrowable().toString()));
        return retry;
    }

    @Bean
    public Bulkhead billingBulkhead(
            @Value("${billing.resilience.bulkhead.max-concurrent-calls:10}") int maxConcurrentCalls,
            @Value("${billing.resilience.bulkhead.max-wait-ms:0}") long maxWaitMs) {

        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(maxConcurrentCalls)
                .maxWaitDuration(Duration.ofMillis(maxWaitMs))
                .build();
        return Bulkhead.of(NAME, config);
    }
}
