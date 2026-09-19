package com.jeep.project.dto;

import com.jeep.project.domain.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
    @NotBlank(message = "Project name is required")
    @Size(min = 2, max = 150, message = "Project name must be between 2 and 150 chars")
    String name,

    @Size(max = 1000, message = "Project description must not exceed 1000 characters")
    String description,

    @NotNull(message = "Project status is required")
    ProjectStatus status
) {
}