package com.jeep.ai.service;

import com.jeep.ai.dto.AiSuggestionRequest;
import com.jeep.ai.dto.AiSuggestionResponse;
import com.jeep.identity.domain.User;
import com.jeep.identity.exception.IdentityException;
import com.jeep.identity.repository.UserRepository;
import com.jeep.organization.repository.OrganizationMemberRepository;
import com.jeep.project.domain.Project;
import com.jeep.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    @Transactional(readOnly = true)
    public AiSuggestionResponse generateSuggestion(String userEmail, AiSuggestionRequest request) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IdentityException("User not found"));

        Project project = projectRepository.findById(request.projectId())
            .orElseThrow(() -> new IdentityException("Project not found"));

        organizationMemberRepository.findByOrganizationAndUser(project.getOrganization(), user)
            .orElseThrow(() -> new IdentityException("User is not a member of this organization"));

        String prompt = request.prompt() == null ? "" : request.prompt();
        String normalizedType = request.type() == null ? "general" : request.type().toLowerCase();

        String suggestion;
        double confidence;

        if (prompt.toLowerCase().contains("late") || prompt.toLowerCase().contains("risk")) {
            suggestion = "Reduce scope, freeze non-critical tasks, and add a protected recovery window for the release milestone.";
            confidence = 0.92;
        } else if (normalizedType.contains("timeline") || normalizedType.contains("schedule")) {
            suggestion = "Split the work into a critical path and a deferred backlog, then re-sequence dependencies to prevent bottlenecks.";
            confidence = 0.88;
        } else {
            suggestion = "Prioritize the highest-risk dependency first, validate progress with a daily checkpoint, and keep the next milestone narrow and measurable.";
            confidence = 0.81;
        }

        return new AiSuggestionResponse(project.getId(), suggestion, confidence, normalizedType);
    }
}
