package com.jeep.repository.github;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Thin client for the public GitHub REST API, used only for linked
 * repositories. Requests are unauthenticated: no token is stored, sent,
 * or required, so the public limit of 60 requests/hour applies. Nothing
 * is cached; each call goes to api.github.com.
 *
 * <p>Rate limits are surfaced as {@link GitHubRateLimitException}
 * (HTTP 429 and 403s that GitHub reports as rate-limit exceeded) and
 * other failures as {@link GitHubApiException}.
 */
@Service
public class GitHubApiClient {

    static final String API_BASE_URL = "https://api.github.com";
    private static final int DEFAULT_PER_PAGE = 10;

    private final RestClient restClient;

    public GitHubApiClient(RestClient githubRestClient) {
        this.restClient = githubRestClient;
    }

    /** Most recent commits, newest first. */
    public List<GitHubCommit> getRecentCommits(String owner, String repo) {
        GitHubCommit[] commits = get(
            "/repos/{owner}/{repo}/commits?per_page=" + DEFAULT_PER_PAGE,
            GitHubCommit[].class, owner, repo);
        return commits == null ? List.of() : List.of(commits);
    }

    /** Open pull requests. */
    public List<GitHubPullRequest> getOpenPullRequests(String owner, String repo) {
        GitHubPullRequest[] pullRequests = get(
            "/repos/{owner}/{repo}/pulls?state=open&per_page=" + DEFAULT_PER_PAGE,
            GitHubPullRequest[].class, owner, repo);
        return pullRequests == null ? List.of() : List.of(pullRequests);
    }

    /** Contributors, most contributions first (GitHub's default order). */
    public List<GitHubContributor> getContributors(String owner, String repo) {
        GitHubContributor[] contributors = get(
            "/repos/{owner}/{repo}/contributors?per_page=" + DEFAULT_PER_PAGE,
            GitHubContributor[].class, owner, repo);
        return contributors == null ? List.of() : List.of(contributors);
    }

    private <T> T get(String uriTemplate, Class<T> type, Object... uriVariables) {
        return restClient.get()
            .uri(uriTemplate, uriVariables)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "JEEP")
            .retrieve()
            .onStatus(status -> status.value() == 403 || status.value() == 429, (request, response) -> {
                if (isRateLimited(response)) {
                    throw new GitHubRateLimitException(resetAt(response));
                }
                throw new GitHubApiException(
                    "GitHub API rejected the request (HTTP 403). The repository may be private or the request may be forbidden.");
            })
            .onStatus(HttpStatusCode::isError, (request, response) -> {
                if (response.getStatusCode().value() == 404) {
                    throw new GitHubApiException(
                        "GitHub repository not found (HTTP 404). It may be private, renamed, or deleted.");
                }
                throw new GitHubApiException(
                    "GitHub API request failed (HTTP " + response.getStatusCode().value() + ").");
            })
            .body(type);
    }

    private boolean isRateLimited(org.springframework.http.client.ClientHttpResponse response) throws IOException {
        if (response.getStatusCode().value() == 429) {
            return true;
        }
        if ("0".equals(response.getHeaders().getFirst("X-RateLimit-Remaining"))) {
            return true;
        }
        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        return body.toLowerCase().contains("rate limit");
    }

    private Instant resetAt(org.springframework.http.client.ClientHttpResponse response) {
        String reset = response.getHeaders().getFirst("X-RateLimit-Reset");
        if (reset == null) {
            return null;
        }
        try {
            return Instant.ofEpochSecond(Long.parseLong(reset));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
