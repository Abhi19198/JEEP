package com.jeep.repository.github;

import com.jeep.repository.domain.RepositoryProvider;

import java.util.List;

/** See {@link GitHubCommitsResponse} for the {@code supported} contract. */
public record GitHubPullRequestsResponse(
    boolean supported,
    String provider,
    String message,
    List<GitHubPullRequestResponse> pullRequests
) {

    public static GitHubPullRequestsResponse supported(RepositoryProvider provider, List<GitHubPullRequestResponse> pullRequests) {
        return new GitHubPullRequestsResponse(true, provider.name(), null, pullRequests);
    }

    public static GitHubPullRequestsResponse unsupported(RepositoryProvider provider, String message) {
        return new GitHubPullRequestsResponse(false, provider.name(), message, List.of());
    }
}
