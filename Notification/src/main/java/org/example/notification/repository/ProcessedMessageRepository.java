package org.example.notification.repository;

import org.example.notification.model.ProcessedMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedMessageRepository extends MongoRepository<ProcessedMessage, String> {
}
