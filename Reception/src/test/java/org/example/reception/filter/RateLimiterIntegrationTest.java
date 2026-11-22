package org.example.reception.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RateLimiterIntegrationTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(2)
                .limitRefreshPeriod(Duration.ofSeconds(10))
                .timeoutDuration(Duration.ZERO)
                .build();
        RateLimiterRegistry registry = RateLimiterRegistry.of(config);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        RateLimitingFilter filter = new RateLimitingFilter(registry, objectMapper);

        this.mockMvc = MockMvcBuilders.standaloneSetup(new DummyApiController())
                .addFilters(filter)
                .build();
    }

    @Test
    void shouldReturnTooManyRequestsWhenRateLimitExceeded() throws Exception {
        mockMvc.perform(get("/api/test-rate-limit"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/test-rate-limit"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/test-rate-limit"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "1"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value("Rate limit exceeded"));
    }

    @Test
    void shouldNotRateLimitActuatorEndpoints() throws Exception {
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk());
        }
    }

    @RestController
    static class DummyApiController {

        @GetMapping(value = "/api/test-rate-limit", produces = MediaType.TEXT_PLAIN_VALUE)
        String testEndpoint() {
            return "ok";
        }

        @GetMapping(value = "/actuator/health", produces = MediaType.TEXT_PLAIN_VALUE)
        String healthEndpoint() {
            return "UP";
        }
    }
}
