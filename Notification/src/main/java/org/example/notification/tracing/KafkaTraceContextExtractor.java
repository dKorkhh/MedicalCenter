package org.example.notification.tracing;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class KafkaTraceContextExtractor {

    private final ObjectProvider<Tracer> tracerProvider;

    public String extractTraceparent(Object traceparentHeader) {
        if (traceparentHeader instanceof ConsumerRecord<?, ?> record) {
            org.apache.kafka.common.header.Header header = record.headers().lastHeader("traceparent");
            if (header != null && header.value() != null) {
                return new String(header.value(), StandardCharsets.UTF_8);
            }
        } else if (traceparentHeader instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        } else if (traceparentHeader instanceof String str) {
            return str;
        } else if (traceparentHeader != null) {
            String str = traceparentHeader.toString();
            if (str.startsWith("00-")) {
                return str;
            }
        }
        return null;
    }

    public TraceContextSession startConsumerSpan(String spanName, String traceparent) {
        Span consumerSpan = null;
        Scope scope = null;

        if (traceparent != null && traceparent.startsWith("00-")) {
            String[] parts = traceparent.split("-");
            if (parts.length >= 4) {
                String traceId = parts[1];
                String parentSpanId = parts[2];
                String flags = parts[3];

                MDC.put("traceId", traceId);
                MDC.put("trace_id", traceId);
                MDC.put("traceparent", traceparent);

                SpanContext remoteCtx = SpanContext.createFromRemoteParent(
                        traceId,
                        parentSpanId,
                        TraceFlags.fromHex(flags, 0),
                        TraceState.getDefault()
                );

                Tracer tracer = tracerProvider.getIfAvailable();
                if (tracer == null) {
                    tracer = GlobalOpenTelemetry.getTracer("org.example.notification");
                }

                consumerSpan = tracer.spanBuilder(spanName)
                        .setParent(Context.current().with(Span.wrap(remoteCtx)))
                        .startSpan();
                scope = consumerSpan.makeCurrent();
            }
        }

        return new TraceContextSession(consumerSpan, scope);
    }
}
