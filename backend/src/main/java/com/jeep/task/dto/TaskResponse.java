package com.jeep.task.dto;

import com.jeep.task.domain.TaskPriority;
import com.jeep.task.domain.TaskStatus;

import java.time.Instant;
import java.util.UUID;

public record TaskResponse(
    UUID id,
    UUID projectId,
    UUID assigneeId,
    String title,
    String description,
    TaskPriority priority,
    TaskStatus status,
    Instant createdAt,
    Instant updatedAt
) {
}
