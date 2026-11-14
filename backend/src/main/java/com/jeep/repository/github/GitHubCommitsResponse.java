package com.jeep.repository.github;

import com.jeep.repository.domain.RepositoryProvider;

import java.util.List;

/**
 * What the API returns for one linked repository's GitHub data.
 * {@code supported} is false — with a human-readable {@code message} and
 * empty lists — when the linked repository is not a github.com link
 * (another provider, or a GitHub provider whose stored URL cannot be
 * parsed as {@code owner/repo}). No data is faked in that case.
 */
public record GitHubCommitsResponse(
    boolean supported,
    String provider,
    String message,
    List<GitHubCommitResponse> commits
) {

    public static GitHubCommitsResponse supported(RepositoryProvider provider, List<GitHubCommitResponse> commits) {
        return new GitHubCommitsResponse(true, provider.name(), null, commits);
    }

    public static GitHubCommitsResponse unsupported(RepositoryProvider provider, String message) {
        return new GitHubCommitsResponse(false, provider.name(), message, List.of());
    }
}
