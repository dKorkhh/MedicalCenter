package org.example.reception.controller;

import org.example.reception.filter.CorrelationIdFilter;
import org.example.reception.idempotency.IdempotencyStore;
import org.example.reception.model.Appointment;
import org.example.reception.repository.AppointmentRepository;
import org.example.reception.support.InMemoryIdempotencyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.data.mongodb.auto-index-creation=false")
class ApiContractIntegrationTest {

    private static final String BODY = """
            {"patientId":"p1","patientFullName":"Ivan Petrenko","doctorId":"d1","doctorFullName":"Dr. Koval",
             "appointmentDateTime":"2030-01-01T10:00:00","roomNumber":"12"}""";

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        IdempotencyStore inMemoryIdempotencyStore() {
            return new InMemoryIdempotencyStore();
        }
    }

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private CorrelationIdFilter correlationIdFilter;
    @MockitoBean
    private AppointmentRepository repository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(correlationIdFilter).build();
    }

    @Test
    void notFoundReturnsProblemJsonAndEchoesCorrelationId() throws Exception {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/appointments/missing").header("X-Correlation-Id", "corr-123"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().string("X-Correlation-Id", "corr-123"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Appointment not found with id: missing"))
                .andExpect(jsonPath("$.correlationId").value("corr-123"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void correlationIdIsGeneratedWhenAbsent() throws Exception {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/appointments/missing"))
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void invalidBodyReturns400ProblemJson() throws Exception {
        mockMvc.perform(post("/api/appointments")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void missingIdempotencyKeyReturns400ProblemJson() throws Exception {
        mockMvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void duplicatePostWithSameIdempotencyKeyCreatesOnlyOneAppointment() throws Exception {
        AtomicReference<Appointment> stored = new AtomicReference<>();
        when(repository.save(any(Appointment.class))).thenAnswer(inv -> {
            Appointment a = inv.getArgument(0);
            a.setId("a-1");
            stored.set(a);
            return a;
        });
        when(repository.findById("a-1")).thenAnswer(inv -> Optional.ofNullable(stored.get()));
        String key = "idem-" + UUID.randomUUID();

        mockMvc.perform(post("/api/appointments").header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("a-1"))
                .andExpect(header().doesNotExist("Idempotent-Replayed"));

        mockMvc.perform(post("/api/appointments").header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("a-1"))
                .andExpect(header().string("Idempotent-Replayed", "true"));

        verify(repository, times(1)).save(any(Appointment.class));

        mockMvc.perform(post("/api/appointments").header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("p1", "p2")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
