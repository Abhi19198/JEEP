package com.jeep.ai.controller;

import com.jeep.ai.dto.AiSuggestionRequest;
import com.jeep.ai.dto.AiSuggestionResponse;
import com.jeep.ai.service.AiAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    @PostMapping("/suggest")
    public ResponseEntity<AiSuggestionResponse> generateSuggestion(
        Authentication authentication,
        @Valid @RequestBody AiSuggestionRequest request
    ) {
        return ResponseEntity.ok(aiAssistantService.generateSuggestion(authentication.getName(), request));
    }
}
