package org.example.medicalcenter.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.example.medicalcenter.client.dto.InvoiceDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfig.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class BillingCircuitBreakerIntegrationTest {

    private WireMockServer wireMockServer;

    @Autowired
    private BillingServiceAdapter billingServiceAdapter;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("billing.service.url", () -> "http://localhost:" + 8081);
    }

    @Test
    void testCircuitBreakerOpensOnFailureStormAndTriggersFallback() {
        stubFor(get(urlEqualTo("/api/invoices/inv-999?delay=0"))
                .willReturn(aResponse()
                        .withStatus(500)));

        for (int i = 0; i < 7; i++) {
            InvoiceDto response = billingServiceAdapter.getInvoice("inv-999", 0);

            if (i >= 4) {
                assertThat(response.getPaymentStatus()).isEqualTo("UNAVAILABLE_TEMPORARILY");
                assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            }
        }

        InvoiceDto fallbackResult = billingServiceAdapter.getInvoice("inv-999", 0);
        assertThat(fallbackResult).isNotNull();
        assertThat(fallbackResult.getPaymentStatus()).isEqualTo("UNAVAILABLE_TEMPORARILY");
    }
}