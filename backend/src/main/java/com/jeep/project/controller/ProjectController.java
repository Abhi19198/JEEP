package com.jeep.project.controller;

import com.jeep.project.dto.CreateProjectRequest;
import com.jeep.project.dto.ProjectResponse;
import com.jeep.project.dto.UpdateProjectRequest;
import com.jeep.project.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
        Authentication authentication,
        @Valid @RequestBody CreateProjectRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(projectService.createProject(authentication.getName(), request));
    }

    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<List<ProjectResponse>> getProjectsForOrganization(Authentication authentication, @PathVariable UUID organizationId) {
        return ResponseEntity.ok(projectService.getProjectsForOrganization(authentication.getName(), organizationId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProject(Authentication authentication, @PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getProject(authentication.getName(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
        Authentication authentication,
        @PathVariable UUID id,
        @Valid @RequestBody UpdateProjectRequest request
    ) {
        return ResponseEntity.ok(projectService.updateProject(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archiveProject(Authentication authentication, @PathVariable UUID id) {
        projectService.archiveProject(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
