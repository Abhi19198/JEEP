package com.jeep.websocket;

import com.jeep.task.dto.TaskResponse;

/**
 * Payload sent to {@code /topic/project/{projectId}} when a task changes.
 * It carries the full updated task, so a subscriber can merge it into its
 * local list without an extra REST round-trip (and without racing the
 * database commit that produced the event).
 */
public record LiveTaskEvent(
    String type,
    TaskResponse task
) {
}
