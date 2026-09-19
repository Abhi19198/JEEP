package com.jeep.repository.controller;

import com.jeep.repository.dto.CreateRepositoryRequest;
import com.jeep.repository.dto.RepositoryResponse;
import com.jeep.repository.service.RepositoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/repositories")
@RequiredArgsConstructor
public class RepositoryController {

    private final RepositoryService repositoryService;

    @PostMapping
    public ResponseEntity<RepositoryResponse> createRepository(
        Authentication authentication,
        @Valid @RequestBody CreateRepositoryRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(repositoryService.createRepository(authentication.getName(), request));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<RepositoryResponse>> getRepositoriesForProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(repositoryService.getRepositoriesForProject(projectId));
    }
}
