package com.jeep.websocket;

import com.jeep.task.dto.TaskResponse;

import java.util.UUID;

/** Published by {@code TaskService} whenever a task is created, updated or deleted. */
public record TaskChangedEvent(
    UUID projectId,
    UUID organizationId,
    TaskChangeType type,
    TaskResponse task
) {
}
