package com.jeep.ai.dto;

import java.util.UUID;

public record AiSuggestionResponse(
    UUID projectId,
    String suggestion,
    double confidence,
    String type
) {
}
