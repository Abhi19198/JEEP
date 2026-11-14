package com.jeep.repository.github;

import java.time.Instant;

/**
 * The unauthenticated GitHub API rate limit (60 requests/hour) was hit.
 * Mapped to HTTP 429 by the global exception handler. No token is stored
 * or used, so the only remedy is to wait until the limit resets.
 */
public class GitHubRateLimitException extends GitHubApiException {

    private final Instant resetAt;

    public GitHubRateLimitException(Instant resetAt) {
        super(resetAt == null
            ? "GitHub API rate limit reached (unauthenticated requests are limited to 60 per hour). Try again later."
            : "GitHub API rate limit reached (unauthenticated requests are limited to 60 per hour). Try again after " + resetAt + ".");
        this.resetAt = resetAt;
    }

    public Instant getResetAt() {
        return resetAt;
    }
}
