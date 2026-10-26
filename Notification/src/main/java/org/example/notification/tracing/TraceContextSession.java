package org.example.notification.tracing;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.MDC;

@Getter
@AllArgsConstructor
public class TraceContextSession implements AutoCloseable {

    private final Span span;
    private final Scope scope;

    public void recordFailure(Throwable throwable) {
        if (span != null) {
            span.recordException(throwable);
            span.setStatus(StatusCode.ERROR, throwable.getMessage());
        }
    }

    @Override
    public void close() {
        if (scope != null) {
            scope.close();
        }
        if (span != null) {
            span.end();
        }
        MDC.remove("traceId");
        MDC.remove("trace_id");
        MDC.remove("traceparent");
    }
}
