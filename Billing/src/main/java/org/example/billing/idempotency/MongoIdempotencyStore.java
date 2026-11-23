package org.example.billing.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MongoIdempotencyStore implements IdempotencyStore {

    private final MongoTemplate mongoTemplate;

    @Override
    public boolean tryAcquire(IdempotencyRecord idempotencyRecord) {
        try {
            mongoTemplate.insert(idempotencyRecord);
            return true;
        } catch (DuplicateKeyException _) {
            return false;
        }
    }

    @Override
    public Optional<IdempotencyRecord> find(String key) {
        return Optional.ofNullable(mongoTemplate.findById(key, IdempotencyRecord.class));
    }

    @Override
    public void complete(String key, String resourceId) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(key)),
                new Update().set("status", IdempotencyRecord.COMPLETED).set("resourceId", resourceId),
                IdempotencyRecord.class);
    }

    @Override
    public void release(String key) {
        mongoTemplate.remove(Query.query(Criteria.where("_id").is(key)), IdempotencyRecord.class);
    }
}
