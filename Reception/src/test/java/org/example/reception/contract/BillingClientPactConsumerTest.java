package org.example.reception.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.example.reception.client.BillingClient;
import org.example.reception.client.dto.InvoiceDto;
import org.example.reception.client.interceptor.CorrelationIdInterceptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "BillingService", pactVersion = PactSpecVersion.V3)
public class BillingClientPactConsumerTest {

    private static final InvoiceDto EXPECTED_INVOICE = InvoiceDto.builder()
            .id("inv-100")
            .appointmentId("app-55")
            .patientId("pat-12")
            .amount(new BigDecimal("650.0"))
            .currency("UAH")
            .paymentStatus("PAID")
            .issuedAt(LocalDateTime.parse("2026-10-03T14:30:00"))
            .paidAt(LocalDateTime.parse("2026-10-03T14:32:00"))
            .build();

    @BeforeAll
    public static void setPactRootDirectory() {
        System.setProperty("pact.rootDir", "../pacts");
    }

    @Pact(consumer = "ReceptionService")
    public RequestResponsePact createInvoiceByIdPact(PactDslWithProvider builder) {
        DslPart invoiceResponseBody = new PactDslJsonBody()
                .stringValue("id", "inv-100")
                .stringValue("appointmentId", "app-55")
                .stringValue("patientId", "pat-12")
                .numberValue("amount", 650.00)
                .stringValue("currency", "UAH")
                .stringValue("paymentStatus", "PAID")
                .stringValue("issuedAt", "2026-10-03T14:30:00")
                .stringValue("paidAt", "2026-10-03T14:32:00");

        return builder
                .given("invoice inv-100 exists")
                .uponReceiving("a request to retrieve invoice by id inv-100")
                .path("/api/invoices/inv-100")
                .matchQuery("delay", "\\d+", "0")
                .method("GET")
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(invoiceResponseBody)
                .toPact();
    }

    @Pact(consumer = "ReceptionService")
    public RequestResponsePact createBatchInvoicesPact(PactDslWithProvider builder) {
        DslPart batchResponseBody = PactDslJsonArray.arrayEachLike()
                .stringValue("id", "inv-100")
                .stringValue("appointmentId", "app-55")
                .stringValue("patientId", "pat-12")
                .numberValue("amount", 650.00)
                .stringValue("currency", "UAH")
                .stringValue("paymentStatus", "PAID")
                .stringValue("issuedAt", "2026-10-03T14:30:00")
                .stringValue("paidAt", "2026-10-03T14:32:00")
                .closeObject();

        return builder
                .given("invoices exist for batch request")
                .uponReceiving("a request to retrieve invoices in batch")
                .path("/api/invoices/batch")
                .method("POST")
                .headers(Map.of("Content-Type", "application/json"))
                .body("[\"inv-100\"]")
                .willRespondWith()
                .status(200)
                .headers(Map.of("Content-Type", "application/json"))
                .body(batchResponseBody)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "createInvoiceByIdPact")
    public void shouldSuccessfullyRetrieveInvoiceByIdMatchingContract(MockServer mockServer) {
        BillingClient billingClient = createBillingClient(mockServer.getUrl());

        InvoiceDto invoice = billingClient.getInvoiceById("inv-100", 0);

        assertThat(invoice).usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .isEqualTo(EXPECTED_INVOICE);
    }

    @Test
    @PactTestFor(pactMethod = "createBatchInvoicesPact")
    public void shouldSuccessfullyRetrieveInvoicesBatchMatchingContract(MockServer mockServer) {
        BillingClient billingClient = createBillingClient(mockServer.getUrl());

        List<InvoiceDto> invoices = billingClient.getInvoicesBatch(List.of("inv-100"));

        assertThat(invoices).usingRecursiveFieldByFieldElementComparator()
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(EXPECTED_INVOICE);
    }

    private BillingClient createBillingClient(String baseUrl) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(new CorrelationIdInterceptor())
                .build();

        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        return proxyFactory.createClient(BillingClient.class);
    }
}
