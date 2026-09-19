package com.jeep.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AiSuggestionRequest(
    @NotNull(message = "Project id is required")
    UUID projectId,

    @NotBlank(message = "Prompt is required")
    String prompt,

    @NotBlank(message = "Suggestion type is required")
    String type
) {
}
