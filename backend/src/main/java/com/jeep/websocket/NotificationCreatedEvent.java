package com.jeep.websocket;

import com.jeep.notification.dto.NotificationResponse;

/** Published by {@code NotificationService} whenever a notification is created for a user. */
public record NotificationCreatedEvent(
    NotificationResponse notification
) {
}
