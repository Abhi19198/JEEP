package com.jeep.analytics.service;

import com.jeep.analytics.dto.OrganizationAnalyticsResponse;
import com.jeep.config.PasswordConfig;
import com.jeep.identity.domain.AccountStatus;
import com.jeep.identity.domain.User;
import com.jeep.identity.domain.UserRole;
import com.jeep.identity.exception.IdentityException;
import com.jeep.identity.repository.UserRepository;
import com.jeep.organization.domain.Organization;
import com.jeep.organization.domain.OrganizationMember;
import com.jeep.organization.domain.OrganizationRole;
import com.jeep.organization.repository.OrganizationMemberRepository;
import com.jeep.organization.repository.OrganizationRepository;
import com.jeep.project.domain.Project;
import com.jeep.project.domain.ProjectStatus;
import com.jeep.project.repository.ProjectRepository;
import com.jeep.shared.cache.CacheService;
import com.jeep.task.domain.Task;
import com.jeep.task.domain.TaskPriority;
import com.jeep.task.domain.TaskStatus;
import com.jeep.task.repository.TaskRepository;
import com.jeep.team.domain.Team;
import com.jeep.team.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({AnalyticsService.class, CacheService.class, PasswordConfig.class})
@ActiveProfiles("test")
class AnalyticsServiceTest {

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMemberRepository organizationMemberRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        teamRepository.deleteAll();
        organizationMemberRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getOrganizationSummary_shouldReturnCountsForOrganization() {
        User user = userRepository.save(User.builder()
            .firstName("Ada")
            .lastName("Lovelace")
            .email("ada@jeep.example")
            .passwordHash(passwordEncoder.encode("StrongPass123!"))
            .status(AccountStatus.ACTIVE)
            .role(UserRole.ADMIN)
            .build());

        Organization organization = organizationRepository.save(Organization.builder()
            .name("JEEP Labs")
            .description("Engineering org")
            .active(true)
            .build());

        organizationMemberRepository.save(OrganizationMember.builder()
            .organization(organization)
            .user(user)
            .role(OrganizationRole.OWNER)
            .build());

        teamRepository.save(Team.builder()
            .organization(organization)
            .name("Platform")
            .build());

        Project project = projectRepository.save(Project.builder()
            .organization(organization)
            .name("Platform Launch")
            .description("Q4 launch")
            .status(ProjectStatus.ACTIVE)
            .build());

        taskRepository.save(Task.builder()
            .project(project)
            .assignee(user)
            .title("Implement SSO")
            .description("Add SSO")
            .priority(TaskPriority.HIGH)
            .status(TaskStatus.TODO)
            .build());

        taskRepository.save(Task.builder()
            .project(project)
            .assignee(user)
            .title("Ship beta")
            .description("Prepare release")
            .priority(TaskPriority.MEDIUM)
            .status(TaskStatus.DONE)
            .build());

        OrganizationAnalyticsResponse response = analyticsService.getOrganizationSummary(organization.getId());

        assertThat(response).isNotNull();
        assertThat(response.organizationId()).isEqualTo(organization.getId());
        assertThat(response.totalMembers()).isEqualTo(1);
        assertThat(response.totalTeams()).isEqualTo(1);
        assertThat(response.totalProjects()).isEqualTo(1);
        assertThat(response.openTasks()).isEqualTo(1);
        assertThat(response.completedTasks()).isEqualTo(1);
    }

    @Test
    void getOrganizationSummary_shouldFailWhenUserIsNotMember() {
        User member = userRepository.save(User.builder()
            .firstName("Ada")
            .lastName("Lovelace")
            .email("ada@jeep.example")
            .passwordHash(passwordEncoder.encode("StrongPass123!"))
            .status(AccountStatus.ACTIVE)
            .role(UserRole.ADMIN)
            .build());

        Organization organization = organizationRepository.save(Organization.builder()
            .name("JEEP Labs")
            .description("Engineering org")
            .active(true)
            .build());

        organizationMemberRepository.save(OrganizationMember.builder()
            .organization(organization)
            .user(member)
            .role(OrganizationRole.OWNER)
            .build());

        userRepository.save(User.builder()
            .firstName("Grace")
            .lastName("Hopper")
            .email("grace@jeep.example")
            .passwordHash(passwordEncoder.encode("StrongPass123!"))
            .status(AccountStatus.ACTIVE)
            .role(UserRole.DEVELOPER)
            .build());

        assertThatThrownBy(() -> analyticsService.getOrganizationSummary("grace@jeep.example", organization.getId()))
            .isInstanceOf(IdentityException.class)
            .hasMessageContaining("not a member");
    }

    @Test
    void getOrganizationSummary_shouldFailForUnknownOrganization() {
        userRepository.save(User.builder()
            .firstName("Ada")
            .lastName("Lovelace")
            .email("ada@jeep.example")
            .passwordHash(passwordEncoder.encode("StrongPass123!"))
            .status(AccountStatus.ACTIVE)
            .role(UserRole.ADMIN)
            .build());

        assertThatThrownBy(() -> analyticsService.getOrganizationSummary("ada@jeep.example", UUID.randomUUID()))
            .isInstanceOf(IdentityException.class)
            .hasMessageContaining("Organization not found");
    }
}
