package com.jeep.repository.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One contributor as returned by {@code GET /repos/{owner}/{repo}/contributors}.
 * Only the fields JEEP displays are modelled; everything else is ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubContributor(
    String login,
    int contributions,
    @JsonProperty("avatar_url") String avatarUrl
) {
}
