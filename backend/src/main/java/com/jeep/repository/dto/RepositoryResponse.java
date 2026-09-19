package com.jeep.repository.dto;

import com.jeep.repository.domain.RepositoryProvider;

import java.time.Instant;
import java.util.UUID;

public record RepositoryResponse(
    UUID id,
    UUID projectId,
    RepositoryProvider provider,
    String remoteUrl,
    String defaultBranch,
    Instant createdAt,
    Instant updatedAt
) {
}
