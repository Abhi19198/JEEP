package com.jeep.repository.service;

import com.jeep.config.PasswordConfig;
import com.jeep.identity.domain.AccountStatus;
import com.jeep.identity.domain.User;
import com.jeep.identity.domain.UserRole;
import com.jeep.identity.repository.UserRepository;
import com.jeep.organization.domain.Organization;
import com.jeep.organization.domain.OrganizationMember;
import com.jeep.organization.domain.OrganizationRole;
import com.jeep.organization.repository.OrganizationMemberRepository;
import com.jeep.organization.repository.OrganizationRepository;
import com.jeep.project.domain.Project;
import com.jeep.project.domain.ProjectStatus;
import com.jeep.project.repository.ProjectRepository;
import com.jeep.repository.domain.RepositoryProvider;
import com.jeep.repository.dto.CreateRepositoryRequest;
import com.jeep.repository.dto.RepositoryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({RepositoryService.class, PasswordConfig.class})
@ActiveProfiles("test")
class RepositoryServiceTest {

    @Autowired
    private RepositoryService repositoryService;

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
    void createRepository_shouldCreateRepositoryForProject() {
        User user = User.builder()
            .firstName("Linus")
            .lastName("Torvalds")
            .email("linus@jeep.example")
            .passwordHash(passwordEncoder.encode("StrongPass123!"))
            .status(AccountStatus.ACTIVE)
            .role(UserRole.DEVELOPER)
            .build();
        user = userRepository.save(user);

        Organization organization = organizationRepository.save(Organization.builder()
            .name("Kernel Team")
            .description("Open source org")
            .active(true)
            .build());

        organizationMemberRepository.save(OrganizationMember.builder()
            .organization(organization)
            .user(user)
            .role(OrganizationRole.MEMBER)
            .build());

        Project project = projectRepository.save(Project.builder()
            .organization(organization)
            .name("Platform Upgrade")
            .description("Repo integration")
            .status(ProjectStatus.ACTIVE)
            .build());

        RepositoryResponse response = repositoryService.createRepository("linus@jeep.example", new CreateRepositoryRequest(
            project.getId(),
            "gitlab",
            "https://gitlab.com/example/platform",
            "main"
        ));

        assertThat(response).isNotNull();
        assertThat(response.provider()).isEqualTo(RepositoryProvider.GITLAB);
        assertThat(response.projectId()).isEqualTo(project.getId());
        assertThat(response.defaultBranch()).isEqualTo("main");
    }
}
