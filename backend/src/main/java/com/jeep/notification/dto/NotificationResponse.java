package com.jeep.notification.dto;

import com.jeep.notification.domain.ChannelType;
import com.jeep.notification.domain.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    UUID userId,
    NotificationType type,
    ChannelType channel,
    String title,
    String message,
    boolean read,
    Instant createdAt
) {
}
