package com.jeep.repository.github;

import com.jeep.identity.domain.AccountStatus;
import com.jeep.identity.domain.User;
import com.jeep.identity.domain.UserRole;
import com.jeep.identity.exception.IdentityException;
import com.jeep.identity.repository.UserRepository;
import com.jeep.organization.domain.Organization;
import com.jeep.organization.domain.OrganizationMember;
import com.jeep.organization.domain.OrganizationRole;
import com.jeep.organization.repository.OrganizationMemberRepository;
import com.jeep.project.domain.Project;
import com.jeep.project.domain.ProjectStatus;
import com.jeep.repository.domain.Repository;
import com.jeep.repository.domain.RepositoryProvider;
import com.jeep.repository.repository.RepositoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubIntegrationServiceTest {

    @Mock
    private RepositoryRepository repositoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationMemberRepository organizationMemberRepository;

    @Mock
    private GitHubApiClient gitHubApiClient;

    private GitHubIntegrationService service;

    private User user;
    private Organization organization;
    private Project project;

    @BeforeEach
    void setUp() {
        service = new GitHubIntegrationService(
            repositoryRepository, userRepository, organizationMemberRepository, gitHubApiClient);

        user = User.builder()
            .firstName("Linus")
            .lastName("Torvalds")
            .email("linus@jeep.example")
            .passwordHash("hashed")
            .status(AccountStatus.ACTIVE)
            .role(UserRole.DEVELOPER)
            .build();

        organization = Organization.builder()
            .name("Kernel Team")
            .description("Open source org")
            .active(true)
            .build();

        project = Project.builder()
            .organization(organization)
            .name("Platform Upgrade")
            .description("Repo integration")
            .status(ProjectStatus.ACTIVE)
            .build();
    }

    private Repository repository(RepositoryProvider provider, String remoteUrl) {
        return Repository.builder()
            .project(project)
            .provider(provider)
            .remoteUrl(remoteUrl)
            .defaultBranch("main")
            .build();
    }

    private void stubMemberAccess(UUID repositoryId, Repository repo) {
        when(userRepository.findByEmail("linus@jeep.example")).thenReturn(Optional.of(user));
        when(repositoryRepository.findById(repositoryId)).thenReturn(Optional.of(repo));
        when(organizationMemberRepository.findByOrganizationAndUser(organization, user))
            .thenReturn(Optional.of(OrganizationMember.builder()
                .organization(organization)
                .user(user)
                .role(OrganizationRole.MEMBER)
                .build()));
    }

    @Test
    void getCommits_shouldReturnMappedCommitsForGitHubRepo() {
        UUID repositoryId = UUID.randomUUID();
        stubMemberAccess(repositoryId, repository(RepositoryProvider.GITHUB, "https://github.com/Abhi19198/JEEP"));

        when(gitHubApiClient.getRecentCommits("Abhi19198", "JEEP")).thenReturn(List.of(
            new GitHubCommit(
                "abc123",
                new GitHubCommit.Commit(
                    "Fix the scheduler\n\nLonger body",
                    new GitHubCommit.CommitAuthor("Linus Torvalds", Instant.parse("2026-09-19T20:38:44Z"))),
                new GitHubCommit.Author("torvalds"),
                "https://github.com/Abhi19198/JEEP/commit/abc123")
        ));

        GitHubCommitsResponse response = service.getCommits("linus@jeep.example", repositoryId);

        assertThat(response.supported()).isTrue();
        assertThat(response.commits()).hasSize(1);
        assertThat(response.commits().get(0).sha()).isEqualTo("abc123");
        assertThat(response.commits().get(0).message()).isEqualTo("Fix the scheduler");
        assertThat(response.commits().get(0).authorName()).isEqualTo("Linus Torvalds");
        assertThat(response.commits().get(0).authorLogin()).isEqualTo("torvalds");
        assertThat(response.commits().get(0).date()).isEqualTo(Instant.parse("2026-09-19T20:38:44Z"));
        verify(gitHubApiClient).getRecentCommits("Abhi19198", "JEEP");
    }

    @Test
    void getPullRequestsAndContributors_shouldReturnMappedDataForGitHubRepo() {
        UUID repositoryId = UUID.randomUUID();
        stubMemberAccess(repositoryId, repository(RepositoryProvider.GITHUB, "git@github.com:Abhi19198/JEEP.git"));

        when(gitHubApiClient.getOpenPullRequests("Abhi19198", "JEEP")).thenReturn(List.of(
            new GitHubPullRequest(7, "Upgrade dependencies", "open",
                new GitHubPullRequest.User("octocat"), Instant.parse("2026-09-17T08:30:00Z"),
                "https://github.com/Abhi19198/JEEP/pull/7")));
        when(gitHubApiClient.getContributors("Abhi19198", "JEEP")).thenReturn(List.of(
            new GitHubContributor("torvalds", 42, "https://avatars.githubusercontent.com/u/1024025?v=4")));

        GitHubPullRequestsResponse prs = service.getPullRequests("linus@jeep.example", repositoryId);
        GitHubContributorsResponse contributors = service.getContributors("linus@jeep.example", repositoryId);

        assertThat(prs.supported()).isTrue();
        assertThat(prs.pullRequests()).hasSize(1);
        assertThat(prs.pullRequests().get(0).number()).isEqualTo(7);
        assertThat(prs.pullRequests().get(0).authorLogin()).isEqualTo("octocat");
        assertThat(contributors.supported()).isTrue();
        assertThat(contributors.contributors()).hasSize(1);
        assertThat(contributors.contributors().get(0).login()).isEqualTo("torvalds");
        assertThat(contributors.contributors().get(0).contributions()).isEqualTo(42);
    }

    @Test
    void getCommits_shouldReturnUnsupportedForNonGitHubProviderWithoutCallingGitHub() {
        UUID repositoryId = UUID.randomUUID();
        stubMemberAccess(repositoryId, repository(RepositoryProvider.GITLAB, "https://gitlab.com/example/platform"));

        GitHubCommitsResponse response = service.getCommits("linus@jeep.example", repositoryId);

        assertThat(response.supported()).isFalse();
        assertThat(response.provider()).isEqualTo("GITLAB");
        assertThat(response.message()).contains("not supported");
        assertThat(response.commits()).isEmpty();
        verify(gitHubApiClient, never()).getRecentCommits("example", "platform");
    }

    @Test
    void getCommits_shouldReturnUnsupportedWhenGitHubUrlCannotBeParsed() {
        UUID repositoryId = UUID.randomUUID();
        stubMemberAccess(repositoryId, repository(RepositoryProvider.GITHUB, "https://github.com/only-owner"));

        GitHubCommitsResponse response = service.getCommits("linus@jeep.example", repositoryId);

        assertThat(response.supported()).isFalse();
        assertThat(response.message()).contains("Could not parse");
        verify(gitHubApiClient, never()).getRecentCommits("only-owner", null);
    }

    @Test
    void getCommits_shouldFailWhenUserIsNotOrganizationMember() {
        UUID repositoryId = UUID.randomUUID();
        Repository repo = repository(RepositoryProvider.GITHUB, "https://github.com/Abhi19198/JEEP");
        when(userRepository.findByEmail("linus@jeep.example")).thenReturn(Optional.of(user));
        when(repositoryRepository.findById(repositoryId)).thenReturn(Optional.of(repo));
        when(organizationMemberRepository.findByOrganizationAndUser(organization, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCommits("linus@jeep.example", repositoryId))
            .isInstanceOf(IdentityException.class)
            .hasMessageContaining("not a member");
        verify(gitHubApiClient, never()).getRecentCommits("Abhi19198", "JEEP");
    }

    @Test
    void getCommits_shouldFailWhenUserOrRepositoryDoesNotExist() {
        UUID repositoryId = UUID.randomUUID();
        when(userRepository.findByEmail("ghost@jeep.example")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCommits("ghost@jeep.example", repositoryId))
            .isInstanceOf(IdentityException.class)
            .hasMessageContaining("User not found");

        when(userRepository.findByEmail("linus@jeep.example")).thenReturn(Optional.of(user));
        when(repositoryRepository.findById(repositoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCommits("linus@jeep.example", repositoryId))
            .isInstanceOf(IdentityException.class)
            .hasMessageContaining("Repository not found");
    }
}
