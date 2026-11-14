package com.jeep.repository.github;

/**
 * The GitHub API call failed for a reason other than the rate limit
 * (for example the repository does not exist, is private, or GitHub
 * returned a 5xx). Mapped to HTTP 502 by the global exception handler.
 */
public class GitHubApiException extends RuntimeException {

    public GitHubApiException(String message) {
        super(message);
    }
}
