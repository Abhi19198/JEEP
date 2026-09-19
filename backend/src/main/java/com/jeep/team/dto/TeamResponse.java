package com.jeep.team.dto;

import java.time.Instant;
import java.util.UUID;

public record TeamResponse(
    UUID id,
    String name,
    UUID organizationId,
    Instant createdAt,
    Instant updatedAt
) {
}
