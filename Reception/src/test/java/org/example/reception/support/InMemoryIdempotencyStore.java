package org.example.reception.support;

import org.example.reception.idempotency.IdempotencyRecord;
import org.example.reception.idempotency.IdempotencyStore;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final Map<String, IdempotencyRecord> records = new ConcurrentHashMap<>();

    @Override
    public boolean tryAcquire(IdempotencyRecord record) {
        return records.putIfAbsent(record.getKey(), record) == null;
    }

    @Override
    public Optional<IdempotencyRecord> find(String key) {
        return Optional.ofNullable(records.get(key));
    }

    @Override
    public void complete(String key, String resourceId) {
        records.computeIfPresent(key, (k, r) -> {
            r.setStatus(IdempotencyRecord.COMPLETED);
            r.setResourceId(resourceId);
            return r;
        });
    }

    @Override
    public void release(String key) {
        records.remove(key);
    }
}
