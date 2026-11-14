package com.jeep.project.service;

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
import com.jeep.project.dto.CreateProjectRequest;
import com.jeep.project.dto.ProjectResponse;
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
@Import({ProjectService.class, PasswordConfig.class})
@ActiveProfiles("test")
class ProjectServiceTest {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMemberRepository organizationMemberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        organizationMemberRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createProject_shouldCreateProjectAndAssociation() {
        User user = User.builder()
            .firstName("Ada")
            .lastName("Lovelace")
            .email("ada@jeep.example")
            .passwordHash(passwordEncoder.encode("StrongPass123!"))
            .status(AccountStatus.ACTIVE)
            .role(UserRole.ADMIN)
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

        ProjectResponse response = projectService.createProject("ada@jeep.example", new CreateProjectRequest(
            organization.getId().toString(),
            "Platform Launch",
            "Q4 launch project"
        ));

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Platform Launch");
        assertThat(response.organizationId()).isEqualTo(organization.getId());
    }

    @Test
    void createProject_shouldFailWhenUserIsNotOrganizationMember() {
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

        assertThatThrownBy(() -> projectService.createProject("grace@jeep.example", new CreateProjectRequest(
            organization.getId().toString(),
            "Sneaky Project",
            "No membership here"
        )))
            .isInstanceOf(IdentityException.class)
            .hasMessageContaining("not a member");
    }

    @Test
    void getProject_shouldFailForCrossOrganizationAccess() {
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

        ProjectResponse created = projectService.createProject("ada@jeep.example", new CreateProjectRequest(
            organization.getId().toString(),
            "Platform Launch",
            "Q4 launch project"
        ));

        userRepository.save(User.builder()
            .firstName("Grace")
            .lastName("Hopper")
            .email("grace@jeep.example")
            .passwordHash(passwordEncoder.encode("StrongPass123!"))
            .status(AccountStatus.ACTIVE)
            .role(UserRole.DEVELOPER)
            .build());

        assertThatThrownBy(() -> projectService.getProject("grace@jeep.example", created.id()))
            .isInstanceOf(IdentityException.class)
            .hasMessageContaining("not a member");
    }
}
