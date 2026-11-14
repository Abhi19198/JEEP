package com.jeep.repository.github;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for the GitHub API client. All HTTP is intercepted by
 * {@link MockRestServiceServer}; the live GitHub API is never hit.
 */
class GitHubApiClientTest {

    private static final String COMMITS_JSON = """
        [
          {
            "sha": "abc123def456",
            "commit": {
              "author": {"name": "Linus Torvalds", "date": "2026-09-19T20:38:44Z"},
              "message": "Fix the scheduler\\n\\nLonger body here"
            },
            "author": {"login": "torvalds"},
            "html_url": "https://github.com/Abhi19198/JEEP/commit/abc123def456"
          },
          {
            "sha": "def456abc123",
            "commit": {
              "author": {"name": "Ada Lovelace", "date": "2026-09-18T10:00:00Z"},
              "message": "Add analytics"
            },
            "author": null,
            "html_url": "https://github.com/Abhi19198/JEEP/commit/def456abc123"
          }
        ]
        """;

    private static final String PULL_REQUESTS_JSON = """
        [
          {
            "number": 7,
            "title": "Upgrade dependencies",
            "state": "open",
            "user": {"login": "octocat"},
            "created_at": "2026-09-17T08:30:00Z",
            "html_url": "https://github.com/Abhi19198/JEEP/pull/7"
          }
        ]
        """;

    private static final String CONTRIBUTORS_JSON = """
        [
          {"login": "torvalds", "contributions": 42, "avatar_url": "https://avatars.githubusercontent.com/u/1024025?v=4"},
          {"login": "octocat", "contributions": 3, "avatar_url": "https://avatars.githubusercontent.com/u/583231?v=4"}
        ]
        """;

    private MockRestServiceServer server;
    private GitHubApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.github.com");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GitHubApiClient(builder.build());
    }

    @Test
    void getRecentCommits_shouldReturnParsedCommits() {
        server.expect(requestTo("https://api.github.com/repos/Abhi19198/JEEP/commits?per_page=10"))
            .andExpect(header("Accept", "application/vnd.github+json"))
            .andRespond(withSuccess(COMMITS_JSON, MediaType.APPLICATION_JSON));

        List<GitHubCommit> commits = client.getRecentCommits("Abhi19198", "JEEP");

        assertThat(commits).hasSize(2);
        assertThat(commits.get(0).sha()).isEqualTo("abc123def456");
        assertThat(commits.get(0).commit().message()).isEqualTo("Fix the scheduler\n\nLonger body here");
        assertThat(commits.get(0).commit().author().name()).isEqualTo("Linus Torvalds");
        assertThat(commits.get(0).commit().author().date()).isEqualTo(Instant.parse("2026-09-19T20:38:44Z"));
        assertThat(commits.get(0).author().login()).isEqualTo("torvalds");
        assertThat(commits.get(1).author()).isNull();
        server.verify();
    }

    @Test
    void getOpenPullRequests_shouldReturnParsedPullRequests() {
        server.expect(requestTo("https://api.github.com/repos/Abhi19198/JEEP/pulls?state=open&per_page=10"))
            .andRespond(withSuccess(PULL_REQUESTS_JSON, MediaType.APPLICATION_JSON));

        List<GitHubPullRequest> pullRequests = client.getOpenPullRequests("Abhi19198", "JEEP");

        assertThat(pullRequests).hasSize(1);
        assertThat(pullRequests.get(0).number()).isEqualTo(7);
        assertThat(pullRequests.get(0).title()).isEqualTo("Upgrade dependencies");
        assertThat(pullRequests.get(0).state()).isEqualTo("open");
        assertThat(pullRequests.get(0).user().login()).isEqualTo("octocat");
        assertThat(pullRequests.get(0).createdAt()).isEqualTo(Instant.parse("2026-09-17T08:30:00Z"));
        server.verify();
    }

    @Test
    void getContributors_shouldReturnParsedContributors() {
        server.expect(requestTo("https://api.github.com/repos/Abhi19198/JEEP/contributors?per_page=10"))
            .andRespond(withSuccess(CONTRIBUTORS_JSON, MediaType.APPLICATION_JSON));

        List<GitHubContributor> contributors = client.getContributors("Abhi19198", "JEEP");

        assertThat(contributors).hasSize(2);
        assertThat(contributors.get(0).login()).isEqualTo("torvalds");
        assertThat(contributors.get(0).contributions()).isEqualTo(42);
        assertThat(contributors.get(0).avatarUrl()).startsWith("https://avatars.githubusercontent.com/");
        server.verify();
    }

    @Test
    void getRecentCommits_shouldThrowRateLimitExceptionOn429() {
        server.expect(requestTo("https://api.github.com/repos/Abhi19198/JEEP/commits?per_page=10"))
            .andRespond(request -> {
                MockClientHttpResponse response =
                    new MockClientHttpResponse(new byte[0], HttpStatus.TOO_MANY_REQUESTS);
                response.getHeaders().set("X-RateLimit-Reset", "1794663688");
                return response;
            });

        assertThatThrownBy(() -> client.getRecentCommits("Abhi19198", "JEEP"))
            .isInstanceOfSatisfying(GitHubRateLimitException.class, ex -> {
                assertThat(ex.getMessage()).contains("rate limit");
                assertThat(ex.getResetAt()).isEqualTo(Instant.ofEpochSecond(1794663688L));
            });
        server.verify();
    }

    @Test
    void getRecentCommits_shouldThrowRateLimitExceptionOn403WithExhaustedLimit() {
        server.expect(requestTo("https://api.github.com/repos/Abhi19198/JEEP/commits?per_page=10"))
            .andRespond(request -> {
                MockClientHttpResponse response = new MockClientHttpResponse(
                    "{\"message\":\"API rate limit exceeded\"}".getBytes(StandardCharsets.UTF_8),
                    HttpStatus.FORBIDDEN);
                response.getHeaders().set("X-RateLimit-Remaining", "0");
                response.getHeaders().set("X-RateLimit-Reset", "1794663688");
                return response;
            });

        assertThatThrownBy(() -> client.getRecentCommits("Abhi19198", "JEEP"))
            .isInstanceOf(GitHubRateLimitException.class)
            .hasMessageContaining("rate limit");
        server.verify();
    }

    @Test
    void getRecentCommits_shouldThrowApiExceptionOn404() {
        server.expect(requestTo("https://api.github.com/repos/Abhi19198/NOPE/commits?per_page=10"))
            .andRespond(request -> new MockClientHttpResponse(
                "{\"message\":\"Not Found\"}".getBytes(StandardCharsets.UTF_8), HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getRecentCommits("Abhi19198", "NOPE"))
            .isInstanceOf(GitHubApiException.class)
            .isNotInstanceOf(GitHubRateLimitException.class)
            .hasMessageContaining("not found");
        server.verify();
    }

    @Test
    void getRecentCommits_shouldThrowApiExceptionOnServerError() {
        server.expect(requestTo("https://api.github.com/repos/Abhi19198/JEEP/commits?per_page=10"))
            .andRespond(request -> new MockClientHttpResponse(
                new byte[0], HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.getRecentCommits("Abhi19198", "JEEP"))
            .isInstanceOf(GitHubApiException.class)
            .hasMessageContaining("500");
        server.verify();
    }

    @Test
    void parseRemoteUrl_shouldParseHttpsGithubUrls() {
        assertThat(GitHubRepoCoordinates.parse("https://github.com/Abhi19198/JEEP"))
            .contains(new GitHubRepoCoordinates("Abhi19198", "JEEP"));
        assertThat(GitHubRepoCoordinates.parse("https://github.com/Abhi19198/JEEP.git"))
            .contains(new GitHubRepoCoordinates("Abhi19198", "JEEP"));
        assertThat(GitHubRepoCoordinates.parse("https://www.github.com/Abhi19198/JEEP/"))
            .contains(new GitHubRepoCoordinates("Abhi19198", "JEEP"));
        assertThat(GitHubRepoCoordinates.parse("http://github.com/owner/repo"))
            .contains(new GitHubRepoCoordinates("owner", "repo"));
    }

    @Test
    void parseRemoteUrl_shouldParseSshGithubUrls() {
        assertThat(GitHubRepoCoordinates.parse("git@github.com:Abhi19198/JEEP.git"))
            .contains(new GitHubRepoCoordinates("Abhi19198", "JEEP"));
        assertThat(GitHubRepoCoordinates.parse("git@github.com:owner/repo"))
            .contains(new GitHubRepoCoordinates("owner", "repo"));
    }

    @Test
    void parseRemoteUrl_shouldReturnEmptyForOtherProvidersAndGarbage() {
        assertThat(GitHubRepoCoordinates.parse("https://gitlab.com/example/platform")).isEqualTo(Optional.empty());
        assertThat(GitHubRepoCoordinates.parse("https://bitbucket.org/example/platform")).isEqualTo(Optional.empty());
        assertThat(GitHubRepoCoordinates.parse("https://github.com/only-owner")).isEqualTo(Optional.empty());
        assertThat(GitHubRepoCoordinates.parse("https://github.com/owner/repo/tree/main")).isEqualTo(Optional.empty());
        assertThat(GitHubRepoCoordinates.parse("not a url")).isEqualTo(Optional.empty());
        assertThat(GitHubRepoCoordinates.parse(null)).isEqualTo(Optional.empty());
        assertThat(GitHubRepoCoordinates.parse("   ")).isEqualTo(Optional.empty());
    }
}
