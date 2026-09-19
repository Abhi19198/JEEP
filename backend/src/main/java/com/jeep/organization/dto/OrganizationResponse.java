package com.jeep.organization.dto;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
    UUID id,
    String name,
    String description,
    boolean active,
    Instant createdAt,
    Instant updatedAt
) {
}
