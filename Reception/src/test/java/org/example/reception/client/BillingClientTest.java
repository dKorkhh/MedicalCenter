package org.example.reception.client;

import com.sun.net.httpserver.HttpServer;
import org.example.reception.client.dto.InvoiceDto;
import org.example.reception.client.interceptor.CorrelationIdInterceptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BillingClientTest {

    private static final Logger log = LoggerFactory.getLogger(BillingClientTest.class);

    private static HttpServer mockServer;
    private static int mockPort;
    private static BillingClient billingClient;
    private static final AtomicReference<String> capturedCorrelationId = new AtomicReference<>();

    @BeforeAll
    static void startMockServer() throws IOException {
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        mockPort = mockServer.getAddress().getPort();

        mockServer.createContext("/api/invoices", exchange -> {
            String correlationId = exchange.getRequestHeaders().getFirst("X-Correlation-Id");
            capturedCorrelationId.set(correlationId);
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();

            log.info("[Mock Server] Received request for URI: {} | Header X-Correlation-Id: {}",
                    exchange.getRequestURI(), correlationId);

            if ((query != null && query.contains("delay=4000")) || path.endsWith("/timeout-inv")) {
                try {
                    log.info("[Mock Server] Delay of 4000ms...");
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            String responseJson = """
                    {
                        "id": "inv-100",
                        "appointmentId": "app-55",
                        "patientId": "pat-12",
                        "amount": 650.00,
                        "currency": "UAH",
                        "paymentStatus": "PAID",
                        "issuedAt": "2026-10-03T14:30:00",
                        "paidAt": "2026-10-03T14:32:00",
                        "unknownFieldFromFutureRelease": "test-tolerant-reader",
                        "systemAuditTag": 998811
                    }
                    """;

            byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        mockServer.start();

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        RestClient restClient = RestClient.builder()
                .baseUrl("http://localhost:" + mockPort)
                .requestFactory(requestFactory)
                .requestInterceptor(new CorrelationIdInterceptor())
                .build();

        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        billingClient = proxyFactory.createClient(BillingClient.class);
    }

    @AfterAll
    static void stopMockServer() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
    }

    @Test
    void testSuccessfulClientCallWithTolerantReaderAndCorrelationId() {
        InvoiceDto invoice = billingClient.getInvoiceById("inv-100", 0);

        InvoiceDto expected = InvoiceDto.builder()
                        .id("inv-100")
                        .patientId("pat-12")
                        .appointmentId("app-55")
                        .amount(new BigDecimal("650.00"))
                        .paymentStatus("PAID")
                        .build();


        assertThat(invoice)
                .usingRecursiveComparison()
                .ignoringExpectedNullFields()
                .isEqualTo(expected);
        assertNotNull(capturedCorrelationId.get());
    }

    @Test
    void testReadTimeoutThrowsResourceAccessException() {
        assertThrows(ResourceAccessException.class,
                () -> billingClient.getInvoiceById("timeout-inv", 4000));
    }
}
