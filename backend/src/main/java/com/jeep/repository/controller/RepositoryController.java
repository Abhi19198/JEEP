package com.jeep.repository.controller;

import com.jeep.repository.dto.CreateRepositoryRequest;
import com.jeep.repository.dto.RepositoryResponse;
import com.jeep.repository.github.GitHubCommitsResponse;
import com.jeep.repository.github.GitHubContributorsResponse;
import com.jeep.repository.github.GitHubIntegrationService;
import com.jeep.repository.github.GitHubPullRequestsResponse;
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
    private final GitHubIntegrationService gitHubIntegrationService;

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

    @GetMapping("/{repositoryId}/github/commits")
    public ResponseEntity<GitHubCommitsResponse> getGitHubCommits(
        Authentication authentication,
        @PathVariable UUID repositoryId
    ) {
        return ResponseEntity.ok(gitHubIntegrationService.getCommits(authentication.getName(), repositoryId));
    }

    @GetMapping("/{repositoryId}/github/pull-requests")
    public ResponseEntity<GitHubPullRequestsResponse> getGitHubPullRequests(
        Authentication authentication,
        @PathVariable UUID repositoryId
    ) {
        return ResponseEntity.ok(gitHubIntegrationService.getPullRequests(authentication.getName(), repositoryId));
    }

    @GetMapping("/{repositoryId}/github/contributors")
    public ResponseEntity<GitHubContributorsResponse> getGitHubContributors(
        Authentication authentication,
        @PathVariable UUID repositoryId
    ) {
        return ResponseEntity.ok(gitHubIntegrationService.getContributors(authentication.getName(), repositoryId));
    }
}
