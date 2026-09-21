package org.example.notification.controller;

import lombok.RequiredArgsConstructor;
import org.example.notification.dto.NotificationResponse;
import org.example.notification.dto.SendNotificationRequest;
import org.example.notification.service.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationResponse> sendNotification(@RequestBody SendNotificationRequest request) {
        NotificationResponse createdNotification = notificationService.sendNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdNotification);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getNotificationById(@PathVariable String id) {
        NotificationResponse notification = notificationService.getNotificationById(id);
        return ResponseEntity.ok(notification);
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(@RequestParam(required = false) String recipientEmail) {
        if (recipientEmail != null && !recipientEmail.isBlank()) {
            return ResponseEntity.ok(notificationService.getNotificationsByRecipientEmail(recipientEmail));
        }
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }
}
