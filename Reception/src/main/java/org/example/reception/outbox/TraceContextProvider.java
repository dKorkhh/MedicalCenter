package org.example.reception.outbox;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TraceContextProvider {

    public String captureCurrentTraceparent() {
        Span currentSpan = Span.current();
        if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
            SpanContext ctx = currentSpan.getSpanContext();
            String traceparent = String.format("00-%s-%s-%s",
                    ctx.getTraceId(),
                    ctx.getSpanId(),
                    ctx.getTraceFlags().asHex());
            log.info("Capturing traceparent for OutboxMessage: {}", traceparent);
            return traceparent;
        }
        return null;
    }
}
