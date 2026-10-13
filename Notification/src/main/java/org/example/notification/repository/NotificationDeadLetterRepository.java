package org.example.notification.repository;

import org.example.notification.model.NotificationDeadLetter;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationDeadLetterRepository extends MongoRepository<NotificationDeadLetter, String> {
}
