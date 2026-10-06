package org.example.billing.exception;

import org.example.billing.filter.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.time.Instant;

public final class ProblemDetails {

    public static final String TYPE_PREFIX = "urn:problem-type:medical-center:";

    private ProblemDetails() {
    }

    public static ProblemDetail of(HttpStatus status, String typeSlug, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(URI.create(TYPE_PREFIX + typeSlug));
        pd.setTitle(title);
        return enrich(pd);
    }

    public static ProblemDetail enrich(ProblemDetail pd) {
        if (pd.getProperties() == null || !pd.getProperties().containsKey("timestamp")) {
            pd.setProperty("timestamp", Instant.now().toString());
        }
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_HEADER);
        if (correlationId != null && (pd.getProperties() == null || !pd.getProperties().containsKey("correlationId"))) {
            pd.setProperty("correlationId", correlationId);
        }
        return pd;
    }
}
