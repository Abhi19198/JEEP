package com.jeep.repository.github;

/** One contributor, flattened for the frontend. */
public record GitHubContributorResponse(
    String login,
    int contributions,
    String avatarUrl
) {
}
