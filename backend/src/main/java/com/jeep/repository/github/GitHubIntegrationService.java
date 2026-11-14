package com.jeep.repository.github;

import com.jeep.identity.domain.User;
import com.jeep.identity.exception.IdentityException;
import com.jeep.identity.repository.UserRepository;
import com.jeep.organization.repository.OrganizationMemberRepository;
import com.jeep.repository.domain.Repository;
import com.jeep.repository.domain.RepositoryProvider;
import com.jeep.repository.repository.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * GitHub activity for linked repositories. Follows the same membership
 * rule as the rest of the repository module: the caller must be a member
 * of the organization that owns the repository's project.
 *
 * <p>Only github.com links return live data. Any other provider — or a
 * GitHub provider whose stored URL cannot be parsed as
 * {@code owner/repo} — returns a {@code supported=false} response with a
 * clear message and empty lists instead of an error or faked data.
 */
@Service
@RequiredArgsConstructor
public class GitHubIntegrationService {

    private final RepositoryRepository repositoryRepository;
    private final UserRepository userRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final GitHubApiClient gitHubApiClient;

    @Transactional(readOnly = true)
    public GitHubCommitsResponse getCommits(String userEmail, UUID repositoryId) {
        Repository repository = loadRepositoryForMember(userEmail, repositoryId);
        Optional<GitHubRepoCoordinates> coordinates = requireGitHubCoordinates(repository);
        if (coordinates.isEmpty()) {
            return GitHubCommitsResponse.unsupported(repository.getProvider(), unsupportedMessage(repository));
        }
        List<GitHubCommitResponse> commits = gitHubApiClient
            .getRecentCommits(coordinates.get().owner(), coordinates.get().repo()).stream()
            .map(commit -> new GitHubCommitResponse(
                commit.sha(),
                firstLine(commit.commit() == null ? null : commit.commit().message()),
                commit.commit() == null || commit.commit().author() == null ? null : commit.commit().author().name(),
                commit.author() == null ? null : commit.author().login(),
                commit.commit() == null || commit.commit().author() == null ? null : commit.commit().author().date(),
                commit.htmlUrl()
            ))
            .toList();
        return GitHubCommitsResponse.supported(repository.getProvider(), commits);
    }

    @Transactional(readOnly = true)
    public GitHubPullRequestsResponse getPullRequests(String userEmail, UUID repositoryId) {
        Repository repository = loadRepositoryForMember(userEmail, repositoryId);
        Optional<GitHubRepoCoordinates> coordinates = requireGitHubCoordinates(repository);
        if (coordinates.isEmpty()) {
            return GitHubPullRequestsResponse.unsupported(repository.getProvider(), unsupportedMessage(repository));
        }
        List<GitHubPullRequestResponse> pullRequests = gitHubApiClient
            .getOpenPullRequests(coordinates.get().owner(), coordinates.get().repo()).stream()
            .map(pr -> new GitHubPullRequestResponse(
                pr.number(),
                pr.title(),
                pr.state(),
                pr.user() == null ? null : pr.user().login(),
                pr.createdAt(),
                pr.htmlUrl()
            ))
            .toList();
        return GitHubPullRequestsResponse.supported(repository.getProvider(), pullRequests);
    }

    @Transactional(readOnly = true)
    public GitHubContributorsResponse getContributors(String userEmail, UUID repositoryId) {
        Repository repository = loadRepositoryForMember(userEmail, repositoryId);
        Optional<GitHubRepoCoordinates> coordinates = requireGitHubCoordinates(repository);
        if (coordinates.isEmpty()) {
            return GitHubContributorsResponse.unsupported(repository.getProvider(), unsupportedMessage(repository));
        }
        List<GitHubContributorResponse> contributors = gitHubApiClient
            .getContributors(coordinates.get().owner(), coordinates.get().repo()).stream()
            .map(contributor -> new GitHubContributorResponse(
                contributor.login(),
                contributor.contributions(),
                contributor.avatarUrl()
            ))
            .toList();
        return GitHubContributorsResponse.supported(repository.getProvider(), contributors);
    }

    private Repository loadRepositoryForMember(String userEmail, UUID repositoryId) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IdentityException("User not found"));

        Repository repository = repositoryRepository.findById(repositoryId)
            .orElseThrow(() -> new IdentityException("Repository not found"));

        organizationMemberRepository.findByOrganizationAndUser(repository.getProject().getOrganization(), user)
            .orElseThrow(() -> new IdentityException("User is not a member of this organization"));

        return repository;
    }

    private Optional<GitHubRepoCoordinates> requireGitHubCoordinates(Repository repository) {
        if (repository.getProvider() != RepositoryProvider.GITHUB) {
            return Optional.empty();
        }
        return GitHubRepoCoordinates.parse(repository.getRemoteUrl());
    }

    private String unsupportedMessage(Repository repository) {
        if (repository.getProvider() != RepositoryProvider.GITHUB) {
            return "GitHub activity is not supported for this provider (" + repository.getProvider()
                + "). Only repositories linked as GitHub can show commits, pull requests, and contributors.";
        }
        return "Could not parse a github.com owner/repository from the stored remote URL ("
            + repository.getRemoteUrl() + "). Link the repository with a URL like https://github.com/owner/repo.";
    }

    private String firstLine(String message) {
        if (message == null) {
            return null;
        }
        String firstLine = message.split("\\R", 2)[0].strip();
        return firstLine.isEmpty() ? message.strip() : firstLine;
    }
}
