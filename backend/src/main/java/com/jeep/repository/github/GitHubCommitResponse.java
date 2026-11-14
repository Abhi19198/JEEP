package com.jeep.repository.github;

import java.time.Instant;

/** One commit, flattened for the frontend. {@code message} is the first line only. */
public record GitHubCommitResponse(
    String sha,
    String message,
    String authorName,
    String authorLogin,
    Instant date,
    String url
) {
}
