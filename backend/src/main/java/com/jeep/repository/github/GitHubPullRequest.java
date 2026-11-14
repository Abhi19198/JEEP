package com.jeep.repository.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

/**
 * One pull request as returned by {@code GET /repos/{owner}/{repo}/pulls}.
 * Only the fields JEEP displays are modelled; everything else is ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubPullRequest(
    int number,
    String title,
    String state,
    User user,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("html_url") String htmlUrl
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(
        String login
    ) {
    }
}
