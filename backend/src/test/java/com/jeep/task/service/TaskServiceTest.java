package com.jeep.task.service;

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
import com.jeep.task.domain.TaskPriority;
import com.jeep.task.domain.TaskStatus;
import com.jeep.task.dto.CreateTaskRequest;
import com.jeep.task.dto.TaskResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({TaskService.class, PasswordConfig.class})
@ActiveProfiles("test")
class TaskServiceTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMemberRepository organizationMemberRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        projectRepository.deleteAll();
        organizationMemberRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createTask_shouldCreateTaskInProject() {
        User user = User.builder()
            .firstName("Grace")
            .lastName("Hopper")
            .email("grace@jeep.example")
            .passwordHash(passwordEncoder.encode("StrongPass123!"))
            .status(AccountStatus.ACTIVE)
            .role(UserRole.DEVELOPER)
            .build();
        user = userRepository.save(user);

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

        Project project = projectRepository.save(Project.builder()
            .organization(organization)
            .name("Platform Upgrade")
            .description("Migration project")
            .status(ProjectStatus.ACTIVE)
            .build());

        TaskResponse response = taskService.createTask("grace@jeep.example", new CreateTaskRequest(
            project.getId(),
            "Implement SSO",
            "Add OAuth login",
            TaskPriority.HIGH,
            user.getId(),
            TaskStatus.TODO
        ));

        assertThat(response).isNotNull();
        assertThat(response.title()).isEqualTo("Implement SSO");
        assertThat(response.projectId()).isEqualTo(project.getId());
        assertThat(response.assigneeId()).isEqualTo(user.getId());
    }

    @Test
    void createTask_shouldFailWhenUserIsNotOrganizationMember() {
        User member = userRepository.save(User.builder()
            .firstName("Grace")
            .lastName("Hopper")
            .email("grace@jeep.example")
            .passwordHash(passwordEncoder.encode("StrongPass123!"))
            .status(AccountStatus.ACTIVE)
            .role(UserRole.DEVELOPER)
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

        Project project = projectRepository.save(Project.builder()
            .organization(organization)
            .name("Platform Upgrade")
            .description("Migration project")
            .status(ProjectStatus.ACTIVE)
            .build());

        userRepository.save(User.builder()
            .firstName("Alan")
            .lastName("Turing")
            .email("alan@jeep.example")
            .passwordHash(passwordEncoder.encode("StrongPass123!"))
            .status(AccountStatus.ACTIVE)
            .role(UserRole.DEVELOPER)
            .build());

        assertThatThrownBy(() -> taskService.createTask("alan@jeep.example", new CreateTaskRequest(
            project.getId(),
            "Sneaky Task",
            "No membership here",
            TaskPriority.HIGH,
            member.getId(),
            TaskStatus.TODO
        )))
            .isInstanceOf(IdentityException.class)
            .hasMessageContaining("not a member");
    }

    @Test
    void getTask_shouldFailForCrossOrganizationAccess() {
        User member = userRepository.save(User.builder()
            .firstName("Grace")
            .lastName("Hopper")
            .email("grace@jeep.example")
            .passwordHash(passwordEncoder.encode("StrongPass123!"))
            .status(AccountStatus.ACTIVE)
            .role(UserRole.DEVELOPER)
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

        Project project = projectRepository.save(Project.builder()
            .organization(organization)
            .name("Platform Upgrade")
            .description("Migration project")
            .status(ProjectStatus.ACTIVE)
            .build());

        TaskResponse created = taskService.createTask("grace@jeep.example", new CreateTaskRequest(
            project.getId(),
            "Implement SSO",
            "Add OAuth login",
            TaskPriority.HIGH,
            member.getId(),
            TaskStatus.TODO
        ));

        userRepository.save(User.builder()
            .firstName("Alan")
            .lastName("Turing")
            .email("alan@jeep.example")
            .passwordHash(passwordEncoder.encode("StrongPass123!"))
            .status(AccountStatus.ACTIVE)
            .role(UserRole.DEVELOPER)
            .build());

        assertThatThrownBy(() -> taskService.getTask("alan@jeep.example", created.id()))
            .isInstanceOf(IdentityException.class)
            .hasMessageContaining("not a member");
    }
}
