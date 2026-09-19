package com.jeep.repository.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateRepositoryRequest(
    @NotNull(message = "Project id is required")
    UUID projectId,

    @NotBlank(message = "Repository provider is required")
    String provider,

    @NotBlank(message = "Remote URL is required")
    @Size(max = 500, message = "Remote URL must not exceed 500 characters")
    String remoteUrl,

    @NotBlank(message = "Default branch is required")
    @Size(max = 100, message = "Default branch name must not exceed 100 characters")
    String defaultBranch
) {
}
