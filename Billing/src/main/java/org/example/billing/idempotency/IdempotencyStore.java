package org.example.billing.idempotency;

import java.util.Optional;

public interface IdempotencyStore {

    boolean tryAcquire(IdempotencyRecord record);

    Optional<IdempotencyRecord> find(String key);

    void complete(String key, String resourceId);

    void release(String key);
}
