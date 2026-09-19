package com.jeep.project.dto;

import com.jeep.project.domain.ProjectStatus;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
    UUID id,
    UUID organizationId,
    String name,
    String description,
    ProjectStatus status,
    Instant createdAt,
    Instant updatedAt
) {
}
