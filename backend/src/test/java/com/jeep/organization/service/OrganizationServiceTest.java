package com.jeep.organization.service;

import com.jeep.config.PasswordConfig;
import com.jeep.identity.domain.AccountStatus;
import com.jeep.identity.domain.User;
import com.jeep.identity.domain.UserRole;
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
}
