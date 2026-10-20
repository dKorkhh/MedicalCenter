package org.example.notification.mapper;

import org.example.notification.dto.NotificationResponse;
import org.example.notification.dto.SendNotificationRequest;
import org.example.notification.model.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.time.LocalDateTime;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, imports = {LocalDateTime.class})
public interface NotificationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deliveryStatus", constant = "SENT")
    @Mapping(target = "sentAt", expression = "java(LocalDateTime.now())")
    Notification toEntity(SendNotificationRequest request);

    NotificationResponse toResponse(Notification notification);
}
