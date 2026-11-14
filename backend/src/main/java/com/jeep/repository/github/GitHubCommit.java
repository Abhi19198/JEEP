package com.jeep.repository.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * One commit as returned by {@code GET /repos/{owner}/{repo}/commits}.
 * Only the fields JEEP displays are modelled; everything else is ignored.
 * Dates are ISO-8601 strings from GitHub and are deserialized to {@link Instant}
 * by Jackson's JavaTime module (registered by Spring Boot).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubCommit(
    String sha,
    Commit commit,
    Author author,
    @JsonProperty("html_url") String htmlUrl
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Commit(
        String message,
        CommitAuthor author
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommitAuthor(
        String name,
        Instant date
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Author(
        String login
    ) {
    }
}
