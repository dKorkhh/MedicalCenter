package org.example.notification.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;
    private String recipientEmail;
    private String recipientPhone;
    private String notificationType;
    private String messageTitle;
    private String messageContent;
    private String deliveryStatus;
    private LocalDateTime sentAt;
}
