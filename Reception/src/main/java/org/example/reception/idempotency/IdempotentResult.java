package org.example.reception.idempotency;

public record IdempotentResult<R>(R value, boolean replayed) {
}
