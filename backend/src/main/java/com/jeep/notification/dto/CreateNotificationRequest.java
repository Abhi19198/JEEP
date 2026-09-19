package com.jeep.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNotificationRequest(
    @NotBlank(message = "Notification type is required")
    String type,

    @NotBlank(message = "Channel is required")
    String channel,

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    String title,

    @Size(max = 1000, message = "Message must not exceed 1000 characters")
    String message
) {
}
