package com.jeep.team.service;

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
import com.jeep.team.dto.CreateTeamRequest;
import com.jeep.team.dto.TeamResponse;
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
@Import({TeamService.class, PasswordConfig.class})
@ActiveProfiles("test")
class TeamServiceTest {

    @Autowired
    private TeamService teamService;

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

    private User saveUser(String firstName, String lastName, String email) {
        return userRepository.save(User.builder()
            .firstName(firstName)
            .lastName(lastName)
            .email(email)
            .passwordHash(passwordEncoder.encode("StrongPass123!"))
            .status(AccountStatus.ACTIVE)
            .role(UserRole.ADMIN)
            .build());
    }

    private Organization saveOrganizationWithMember(User user) {
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

        return organization;
    }

    @Test
    void createTeam_shouldCreateTeamForOrganization() {
        User user = saveUser("Ada", "Lovelace", "ada@jeep.example");
        Organization organization = saveOrganizationWithMember(user);

        TeamResponse response = teamService.createTeam("ada@jeep.example", new CreateTeamRequest(
            "Platform",
            organization.getId().toString()
        ));

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("Platform");
        assertThat(response.organizationId()).isEqualTo(organization.getId());
    }

    @Test
    void createTeam_shouldFailWhenUserIsNotOrganizationMember() {
        User member = saveUser("Ada", "Lovelace", "ada@jeep.example");
        Organization organization = saveOrganizationWithMember(member);
        saveUser("Grace", "Hopper", "grace@jeep.example");

        assertThatThrownBy(() -> teamService.createTeam("grace@jeep.example", new CreateTeamRequest(
            "Sneaky Team",
            organization.getId().toString()
        )))
            .isInstanceOf(IdentityException.class)
            .hasMessageContaining("not a member");
    }

    @Test
    void createTeam_shouldFailForUnknownOrganization() {
        saveUser("Ada", "Lovelace", "ada@jeep.example");

        assertThatThrownBy(() -> teamService.createTeam("ada@jeep.example", new CreateTeamRequest(
            "Platform",
            UUID.randomUUID().toString()
        )))
            .isInstanceOf(IdentityException.class)
            .hasMessageContaining("Organization not found");
    }
}
