package com.jeep.organization.service;

import com.jeep.config.PasswordConfig;
import com.jeep.identity.domain.AccountStatus;
import com.jeep.identity.domain.User;
import com.jeep.identity.domain.UserRole;
import com.jeep.identity.exception.IdentityException;
import com.jeep.identity.repository.UserRepository;
import com.jeep.identity.service.TokenService;
import com.jeep.organization.dto.CreateOrganizationRequest;
import com.jeep.organization.dto.OrganizationResponse;
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
@Import({OrganizationService.class, PasswordConfig.class, TokenService.class})
@ActiveProfiles("test")
class OrganizationServiceTest {

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void createOrganization_shouldCreateOrganizationAndOwnIt() {
        User user = User.builder()
            .firstName("Ada")
            .lastName("Lovelace")
            .email("ada@jeep.example")
            .passwordHash(passwordEncoder.encode("StrongPass123!"))
            .status(AccountStatus.ACTIVE)
            .role(UserRole.ADMIN)
            .build();
        userRepository.save(user);

        OrganizationResponse response = organizationService.createOrganization("ada@jeep.example", new CreateOrganizationRequest("JEEP Labs", "Engineering org"));

        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo("JEEP Labs");
        assertThat(response.active()).isTrue();
    }

    @Test
    void getOrganization_shouldFailWhenUserIsNotMember() {
        User owner = User.builder()
            .firstName("Ada")
            .lastName("Lovelace")
            .email("ada@jeep.example")
            .passwordHash(passwordEncoder.encode("StrongPass123!"))
            .status(AccountStatus.ACTIVE)
            .role(UserRole.ADMIN)
            .build();
        userRepository.save(owner);

        OrganizationResponse created = organizationService.createOrganization("ada@jeep.example", new CreateOrganizationRequest("JEEP Labs", "Engineering org"));

        userRepository.save(User.builder()
            .firstName("Grace")
            .lastName("Hopper")
            .email("grace@jeep.example")
            .passwordHash(passwordEncoder.encode("StrongPass123!"))
            .status(AccountStatus.ACTIVE)
            .role(UserRole.DEVELOPER)
            .build());

        assertThatThrownBy(() -> organizationService.getOrganization("grace@jeep.example", created.id()))
            .isInstanceOf(IdentityException.class)
            .hasMessageContaining("not a member");
    }

    @Test
    void createOrganization_shouldFailForUnknownUser() {
        assertThatThrownBy(() -> organizationService.createOrganization("ghost@jeep.example", new CreateOrganizationRequest("Ghost Org", "No such user")))
            .isInstanceOf(IdentityException.class)
            .hasMessageContaining("User not found");
    }
}
