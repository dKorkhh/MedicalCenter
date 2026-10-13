package org.example.reception.outbox;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxMessageRepository extends MongoRepository<OutboxMessage, String> {

    List<OutboxMessage> findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus status);

    List<OutboxMessage> findByStatus(OutboxStatus status);

    List<OutboxMessage> findByAggregateId(String aggregateId);
}
