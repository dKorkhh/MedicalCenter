package org.example.billing.idempotency;

public record IdempotentResult<R>(R value, boolean replayed) {
}
