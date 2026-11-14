package com.jeep.repository.github;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The {@code owner/repo} coordinates parsed out of a stored repository link.
 * Only github.com links can be parsed; anything else returns empty and the
 * caller reports the provider as not supported instead of failing.
 *
 * <p>Accepted shapes (with or without a trailing slash or {@code .git} suffix):
 * <ul>
 *   <li>{@code https://github.com/owner/repo}</li>
 *   <li>{@code https://www.github.com/owner/repo.git}</li>
 *   <li>{@code git@github.com:owner/repo.git}</li>
 * </ul>
 */
public record GitHubRepoCoordinates(String owner, String repo) {

    private static final Pattern HTTPS_URL =
        Pattern.compile("^https?://(?:www\\.)?github\\.com/([^/\\s]+)/([^/\\s]+?)(?:\\.git)?/?$");
    private static final Pattern SSH_URL =
        Pattern.compile("^git@github\\.com:([^/\\s]+)/([^/\\s]+?)(?:\\.git)?$");

    public static Optional<GitHubRepoCoordinates> parse(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.isBlank()) {
            return Optional.empty();
        }
        String url = remoteUrl.strip();
        Matcher https = HTTPS_URL.matcher(url);
        if (https.matches()) {
            return Optional.of(new GitHubRepoCoordinates(https.group(1), https.group(2)));
        }
        Matcher ssh = SSH_URL.matcher(url);
        if (ssh.matches()) {
            return Optional.of(new GitHubRepoCoordinates(ssh.group(1), ssh.group(2)));
        }
        return Optional.empty();
    }
}
