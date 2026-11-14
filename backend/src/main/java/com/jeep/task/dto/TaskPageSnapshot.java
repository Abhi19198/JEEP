package com.jeep.task.dto;

import java.util.List;

/**
 * Serializable snapshot of one task-list page, as stored in the Redis
 * cache. A Spring {@code Page} itself is not cached: its Jackson
 * round-trip is fragile, so the service caches this plain record and
 * rebuilds a {@code Page} from it on a hit.
 */
public record TaskPageSnapshot(
    List<TaskResponse> content,
    int totalPages,
    long totalElements,
    int number,
    int size
) {
}
