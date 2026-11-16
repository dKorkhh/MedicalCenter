package org.example.billing.contract;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.example.billing.dto.InvoiceResponse;
import org.example.billing.service.InvoiceAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@Provider("BillingService")
@PactFolder("../pacts")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.data.mongodb.auto-index-creation=false",
        "spring.kafka.listener.auto-startup=false",
        "spring.grpc.server.port=0",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
public class BillingInvoiceContractProviderTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private InvoiceAccessService invoiceAccessService;

    @TestConfiguration
    static class ContractTestSecurityConfig {

        @Bean
        @Order(Ordered.HIGHEST_PRECEDENCE)
        public SecurityFilterChain contractTestSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .securityMatcher("/api/invoices/**")
                    .csrf(AbstractHttpConfigurer::disable)
                    .addFilterBefore((request, response, chain) -> {
                        Authentication authentication = new UsernamePasswordAuthenticationToken(
                                "test-user", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
                        SecurityContext context = SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(authentication);
                        SecurityContextHolder.setContext(context);
                        chain.doFilter(request, response);
                    }, AuthorizationFilter.class)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    @BeforeEach
    void setupTarget(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget("localhost", port));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPactInteractions(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("invoice inv-100 exists")
    void setupInvoiceExistsState() {
        InvoiceResponse response = InvoiceResponse.builder()
                .id("inv-100")
                .appointmentId("app-55")
                .patientId("pat-12")
                .amount(new BigDecimal("650.00"))
                .currency("UAH")
                .paymentStatus("PAID")
                .issuedAt(LocalDateTime.parse("2026-10-03T14:30:00"))
                .paidAt(LocalDateTime.parse("2026-10-03T14:32:00"))
                .build();

        when(invoiceAccessService.getInvoiceVisibleToCurrentUser("inv-100")).thenReturn(response);
    }

    @State("invoices exist for batch request")
    void setupInvoicesBatchState() {
        InvoiceResponse response = InvoiceResponse.builder()
                .id("inv-100")
                .appointmentId("app-55")
                .patientId("pat-12")
                .amount(new BigDecimal("650.00"))
                .currency("UAH")
                .paymentStatus("PAID")
                .issuedAt(LocalDateTime.parse("2026-10-03T14:30:00"))
                .paidAt(LocalDateTime.parse("2026-10-03T14:32:00"))
                .build();

        when(invoiceAccessService.getInvoicesBatchVisibleToCurrentUser(anyList())).thenReturn(List.of(response));
    }
}
