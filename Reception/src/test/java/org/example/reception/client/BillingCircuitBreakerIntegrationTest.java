package org.example.reception.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.example.reception.client.dto.InvoiceDto;
import org.example.reception.dto.AppointmentDetailsResponse;
import org.example.reception.model.Appointment;
import org.example.reception.repository.AppointmentRepository;
import org.example.reception.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.data.mongodb.auto-index-creation=false",
        "billing.resilience.retry.wait-duration-ms=10",
        "billing.resilience.bulkhead.max-concurrent-calls=2"
})
class BillingCircuitBreakerIntegrationTest {

    private static final String INVOICE_JSON = """
            {"id":"%s","appointmentId":"a-1","patientId":"p-1","amount":650.00,"currency":"UAH","paymentStatus":"PAID"}""";

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("billing.service.url", wm::baseUrl);
    }

    @Autowired
    private BillingServiceAdapter adapter;
    @Autowired
    private CircuitBreaker circuitBreaker;
    @Autowired
    private Bulkhead bulkhead;
    @Autowired
    private AppointmentService appointmentService;
    @MockitoBean
    private AppointmentRepository appointmentRepository;

    @BeforeEach
    void resetState() {
        wm.resetAll();
        circuitBreaker.reset();
    }

    private int requestsTo(String path) {
        return wm.findAll(getRequestedFor(urlPathEqualTo(path))).size();
    }

    @Test
    void failureStormOpensCircuitBreakerAndSwitchesToFallback() {
        String path = "/api/invoices/inv-999";
        wm.stubFor(get(urlPathEqualTo(path)).willReturn(serverError()));

        for (int i = 0; i < 4; i++) {
            InvoiceDto response = adapter.getInvoice("inv-999", 0);
            assertThat(response.paymentStatus()).isEqualTo("UNAVAILABLE_TEMPORARILY");
            assertThat(response.amount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.id()).isEqualTo("inv-999");
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        int requestsWhenOpened = requestsTo(path);
        assertThat(requestsWhenOpened).isEqualTo(5);

        for (int i = 0; i < 3; i++) {
            assertThat(adapter.getInvoice("inv-999", 0).paymentStatus()).isEqualTo("UNAVAILABLE_TEMPORARILY");
        }
        assertThat(requestsTo(path)).isEqualTo(requestsWhenOpened);
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void circuitBreakerRecoversThroughHalfOpenWhenBillingIsBackOnline() {
        wm.stubFor(get(urlPathEqualTo("/api/invoices/inv-1")).willReturn(serverError()));
        for (int i = 0; i < 3; i++) {
            adapter.getInvoice("inv-1", 0);
        }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        wm.resetAll();
        wm.stubFor(get(urlPathEqualTo("/api/invoices/inv-1"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(INVOICE_JSON.formatted("inv-1"))));
        circuitBreaker.transitionToHalfOpenState();

        for (int i = 0; i < 3; i++) {
            assertThat(adapter.getInvoice("inv-1", 0).paymentStatus()).isEqualTo("PAID");
        }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void retryRepeatsFailedCallAndReturnsRealResponseOnThirdAttempt() {
        String path = "/api/invoices/inv-retry";
        wm.stubFor(get(urlPathEqualTo(path)).inScenario("flaky").whenScenarioStateIs(Scenario.STARTED)
                .willReturn(serverError()).willSetStateTo("second"));
        wm.stubFor(get(urlPathEqualTo(path)).inScenario("flaky").whenScenarioStateIs("second")
                .willReturn(serverError()).willSetStateTo("third"));
        wm.stubFor(get(urlPathEqualTo(path)).inScenario("flaky").whenScenarioStateIs("third")
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody(INVOICE_JSON.formatted("inv-retry"))));

        InvoiceDto invoice = adapter.getInvoice("inv-retry", 0);

        assertThat(invoice.paymentStatus()).isEqualTo("PAID");
        assertThat(requestsTo(path)).isEqualTo(3);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void clientErrorProblemJsonIsParsedNotRetriedAndDoesNotOpenCircuit() {
        String path = "/api/invoices/missing";
        wm.stubFor(get(urlPathEqualTo(path)).willReturn(aResponse()
                .withStatus(404)
                .withHeader("Content-Type", "application/problem+json")
                .withBody("""
                        {"type":"about:blank","title":"Not Found","status":404,"detail":"Invoice not found with id: missing"}""")));

        MDC.put("X-Correlation-Id", "corr-it-1");
        MDC.put("traceparent", "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01");
        try {
            assertThatThrownBy(() -> adapter.getInvoice("missing", 0))
                    .isInstanceOfSatisfying(DownstreamProblemException.class, ex -> {
                        assertThat(ex.getStatus()).isEqualTo(404);
                        assertThat(ex.getProblem()).isNotNull();
                        assertThat(ex.getProblem().detail()).isEqualTo("Invoice not found with id: missing");
                    });
        } finally {
            MDC.clear();
        }

        wm.verify(exactly(1), getRequestedFor(urlPathEqualTo(path))
                .withHeader("X-Correlation-Id", equalTo("corr-it-1"))
                .withHeader("traceparent", equalTo("00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01")));
        assertThat(requestsTo(path)).isEqualTo(1);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Test
    void bulkheadLimitsConcurrentCallsAndRejectedCallsGetFallback() throws Exception {
        String path = "/api/invoices/slow";
        wm.stubFor(get(urlPathEqualTo(path)).willReturn(aResponse()
                .withFixedDelay(1000)
                .withHeader("Content-Type", "application/json")
                .withBody(INVOICE_JSON.formatted("slow"))));

        int threads = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<InvoiceDto>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Callable<InvoiceDto> task = () -> {
                start.await();
                return adapter.getInvoice("slow", 0);
            };
            futures.add(pool.submit(task));
        }
        start.countDown();

        long real = 0;
        long fallback = 0;
        for (Future<InvoiceDto> f : futures) {
            InvoiceDto dto = f.get();
            if ("PAID".equals(dto.paymentStatus())) {
                real++;
            } else if ("UNAVAILABLE_TEMPORARILY".equals(dto.paymentStatus())) {
                fallback++;
            }
        }
        pool.shutdown();

        assertThat(real).isGreaterThanOrEqualTo(1);
        assertThat(fallback).isGreaterThanOrEqualTo(1);
        assertThat(real + fallback).isEqualTo(threads);
        assertThat(requestsTo(path)).isEqualTo((int) real);          // відхилені виклики не дійшли до Billing
        assertThat(bulkhead.getMetrics().getAvailableConcurrentCalls()).isEqualTo(2);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED); // відмова Bulkhead не відкриває ланцюг
    }

    @Test
    void batchEndpointFetchesAllInvoicesWithSingleHttpRequest() {
        wm.stubFor(post(urlPathEqualTo("/api/invoices/batch")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json")
                .withBody("[" + INVOICE_JSON.formatted("inv-1") + "," + INVOICE_JSON.formatted("inv-2") + ","
                        + INVOICE_JSON.formatted("inv-3") + "]")));

        List<InvoiceDto> invoices = adapter.getInvoicesBatch(List.of("inv-1", "inv-2", "inv-3", "inv-2"));

        assertThat(invoices).extracting(InvoiceDto::id).containsExactly("inv-1", "inv-2", "inv-3");
        wm.verify(exactly(1), postRequestedFor(urlPathEqualTo("/api/invoices/batch"))
                .withRequestBody(equalToJson("[\"inv-1\",\"inv-2\",\"inv-3\"]")));   // дублікати прибрано
    }

    @Test
    void appointmentsWithInvoicesUsesOneBatchCallInsteadOfNPlusOne() {
        List<Appointment> appointments = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            appointments.add(Appointment.builder().id("a-" + i).patientId("p-1").invoiceId("inv-" + i).build());
        }
        when(appointmentRepository.findAll()).thenReturn(appointments);
        StringBuilder body = new StringBuilder("[");
        for (int i = 1; i <= 5; i++) {
            body.append(i > 1 ? "," : "").append(INVOICE_JSON.formatted("inv-" + i));
        }
        wm.stubFor(post(urlPathEqualTo("/api/invoices/batch")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json").withBody(body.append("]").toString())));

        List<AppointmentDetailsResponse> details = appointmentService.getAppointmentDetails(null);

        assertThat(details).hasSize(5);
        assertThat(details).allSatisfy(d -> assertThat(d.invoice().paymentStatus()).isEqualTo("PAID"));
        wm.verify(exactly(1), postRequestedFor(urlPathEqualTo("/api/invoices/batch")));   // 1 запит замість 5
        assertThat(wm.findAll(getRequestedFor(urlPathEqualTo("/api/invoices/inv-1")))).isEmpty();
    }

    @Test
    void batchFallbackReturnsSafePlaceholdersWhenBillingIsDown() {
        wm.stubFor(post(urlPathEqualTo("/api/invoices/batch")).willReturn(serverError()));

        List<InvoiceDto> invoices = adapter.getInvoicesBatch(List.of("inv-1", "inv-2"));

        assertThat(invoices).hasSize(2);
        assertThat(invoices).allSatisfy(i -> {
            assertThat(i.paymentStatus()).isEqualTo("UNAVAILABLE_TEMPORARILY");
            assertThat(i.amount()).isEqualByComparingTo(BigDecimal.ZERO);
        });
    }
}
