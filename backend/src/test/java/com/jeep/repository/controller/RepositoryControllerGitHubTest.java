package com.jeep.repository.controller;

import com.jeep.identity.service.TokenService;
import com.jeep.repository.domain.RepositoryProvider;
import com.jeep.repository.github.GitHubApiException;
import com.jeep.repository.github.GitHubCommitResponse;
import com.jeep.repository.github.GitHubCommitsResponse;
import com.jeep.repository.github.GitHubContributorsResponse;
import com.jeep.repository.github.GitHubIntegrationService;
import com.jeep.repository.github.GitHubPullRequestsResponse;
import com.jeep.repository.github.GitHubRateLimitException;
import com.jeep.repository.service.RepositoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoint tests for the GitHub activity routes. Security filters are off;
 * the service layer (membership checks, provider handling) is covered by
 * {@code GitHubIntegrationServiceTest}.
 */
@WebMvcTest(RepositoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class RepositoryControllerGitHubTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RepositoryService repositoryService;

    @MockBean
    private GitHubIntegrationService gitHubIntegrationService;

    // The web slice also loads the app's SecurityConfig, whose JWT filter
    // needs a TokenService bean; it is mocked because no real token is
    // involved here (filters are disabled via addFilters = false anyway).
    @MockBean
    private TokenService tokenService;

    // spring-security-test is not a dependency, so the Authentication the
    // controller reads is supplied as the request principal instead:
    // Spring MVC resolves a bare Authentication parameter from
    // request.getUserPrincipal() via PrincipalMethodArgumentResolver.
    private static RequestPostProcessor asUser() {
        return request -> {
            request.setUserPrincipal(
                new UsernamePasswordAuthenticationToken("linus@jeep.example", null, List.of()));
            return request;
        };
    }

    @Test
    void getGitHubCommits_shouldReturnCommits() throws Exception {
        UUID repositoryId = UUID.randomUUID();
        when(gitHubIntegrationService.getCommits(eq("linus@jeep.example"), eq(repositoryId)))
            .thenReturn(GitHubCommitsResponse.supported(RepositoryProvider.GITHUB, List.of(
                new GitHubCommitResponse("abc123", "Fix the scheduler", "Linus Torvalds", "torvalds",
                    Instant.parse("2026-09-19T20:38:44Z"), "https://github.com/Abhi19198/JEEP/commit/abc123"))));

        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/github/commits", repositoryId).with(asUser()))
            .andDo(result -> { if (result.getResolvedException() != null) result.getResolvedException().printStackTrace(); })
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.supported").value(true))
            .andExpect(jsonPath("$.commits[0].sha").value("abc123"))
            .andExpect(jsonPath("$.commits[0].message").value("Fix the scheduler"))
            .andExpect(jsonPath("$.commits[0].authorLogin").value("torvalds"));
    }

    @Test
    void getGitHubPullRequests_shouldReturnUnsupportedForNonGitHubProvider() throws Exception {
        UUID repositoryId = UUID.randomUUID();
        when(gitHubIntegrationService.getPullRequests(eq("linus@jeep.example"), eq(repositoryId)))
            .thenReturn(GitHubPullRequestsResponse.unsupported(RepositoryProvider.GITLAB,
                "GitHub activity is not supported for this provider (GITLAB)."));

        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/github/pull-requests", repositoryId).with(asUser()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.supported").value(false))
            .andExpect(jsonPath("$.provider").value("GITLAB"))
            .andExpect(jsonPath("$.pullRequests").isEmpty());
    }

    @Test
    void getGitHubContributors_shouldReturnContributors() throws Exception {
        UUID repositoryId = UUID.randomUUID();
        when(gitHubIntegrationService.getContributors(eq("linus@jeep.example"), eq(repositoryId)))
            .thenReturn(GitHubContributorsResponse.supported(RepositoryProvider.GITHUB, List.of()));

        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/github/contributors", repositoryId).with(asUser()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.supported").value(true))
            .andExpect(jsonPath("$.contributors").isEmpty());
    }

    @Test
    void getGitHubCommits_shouldReturn429WhenRateLimited() throws Exception {
        UUID repositoryId = UUID.randomUUID();
        when(gitHubIntegrationService.getCommits(any(), eq(repositoryId)))
            .thenThrow(new GitHubRateLimitException(null));

        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/github/commits", repositoryId).with(asUser()))
            .andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.error").value("GITHUB_RATE_LIMITED"))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("rate limit")));
    }

    @Test
    void getGitHubCommits_shouldReturn502WhenGitHubApiFails() throws Exception {
        UUID repositoryId = UUID.randomUUID();
        when(gitHubIntegrationService.getCommits(any(), eq(repositoryId)))
            .thenThrow(new GitHubApiException("GitHub repository not found (HTTP 404)."));

        mockMvc.perform(get("/api/v1/repositories/{repositoryId}/github/commits", repositoryId).with(asUser()))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.error").value("GITHUB_API_ERROR"));
    }
}
