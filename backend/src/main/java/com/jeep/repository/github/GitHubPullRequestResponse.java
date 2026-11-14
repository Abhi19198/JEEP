package com.jeep.repository.github;

import java.time.Instant;

/** One open pull request, flattened for the frontend. */
public record GitHubPullRequestResponse(
    int number,
    String title,
    String state,
    String authorLogin,
    Instant createdAt,
    String url
) {
}
