package com.jeep.repository.github;

import com.jeep.repository.domain.RepositoryProvider;

import java.util.List;

/** See {@link GitHubCommitsResponse} for the {@code supported} contract. */
public record GitHubContributorsResponse(
    boolean supported,
    String provider,
    String message,
    List<GitHubContributorResponse> contributors
) {

    public static GitHubContributorsResponse supported(RepositoryProvider provider, List<GitHubContributorResponse> contributors) {
        return new GitHubContributorsResponse(true, provider.name(), null, contributors);
    }

    public static GitHubContributorsResponse unsupported(RepositoryProvider provider, String message) {
        return new GitHubContributorsResponse(false, provider.name(), message, List.of());
    }
}
