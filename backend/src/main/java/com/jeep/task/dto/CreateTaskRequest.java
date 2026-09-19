package com.jeep.task.dto;

import com.jeep.task.domain.TaskPriority;
import com.jeep.task.domain.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateTaskRequest(
    @NotNull(message = "Project id is required")
    UUID projectId,

    @NotBlank(message = "Task title is required")
    @Size(min = 2, max = 200, message = "Task title must be between 2 and 200 chars")
    String title,

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    String description,

    @NotNull(message = "Priority is required")
    TaskPriority priority,

    UUID assigneeId,

    @NotNull(message = "Status is required")
    TaskStatus status
) {
}
