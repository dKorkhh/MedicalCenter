package org.example.reception.client.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.UUID;

public class CorrelationIdInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdInterceptor.class);
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String TRACEPARENT_HEADER = "traceparent";

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        if (!request.getHeaders().containsHeader(CORRELATION_ID_HEADER)) {
            String correlationId = MDC.get(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            request.getHeaders().add(CORRELATION_ID_HEADER, correlationId);
        }
        String traceparent = MDC.get(TRACEPARENT_HEADER);
        if (traceparent != null && !request.getHeaders().containsHeader(TRACEPARENT_HEADER)) {
            request.getHeaders().add(TRACEPARENT_HEADER, traceparent);
        }

        log.info("[HTTP Client] Outgoing request: {} {} | {}: {}", request.getMethod(), request.getURI(),
                CORRELATION_ID_HEADER, request.getHeaders().getFirst(CORRELATION_ID_HEADER));
        return execution.execute(request, body);
    }
}
