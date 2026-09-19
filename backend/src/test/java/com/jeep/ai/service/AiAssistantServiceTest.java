package com.jeep.ai.service;

import com.jeep.ai.dto.AiSuggestionRequest;
import com.jeep.ai.dto.AiSuggestionResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({AiAssistantService.class, PasswordConfig.class})
@ActiveProfiles("test")
class AiAssistantServiceTest {

    @Autowired
    private AiAssistantService aiAssistantService;

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
    void generateSuggestion_shouldReturnActionableSuggestion() {
        User user = userRepository.save(User.builder()
            .firstName("Alan")
            .lastName("Turing")
            .email("alan@jeep.example")
            .passwordHash(passwordEncoder.encode("StrongPass123!"))
            .status(AccountStatus.ACTIVE)
            .role(UserRole.PROJECT_MANAGER)
            .build());

        Organization organization = organizationRepository.save(Organization.builder()
            .name("AI Team")
            .description("Research org")
            .active(true)
            .build());

        organizationMemberRepository.save(OrganizationMember.builder()
            .organization(organization)
            .user(user)
            .role(OrganizationRole.ADMIN)
            .build());

        Project project = projectRepository.save(Project.builder()
            .organization(organization)
            .name("ML Pipeline")
            .description("Build data pipeline")
            .status(ProjectStatus.ACTIVE)
            .build());

        AiSuggestionResponse response = aiAssistantService.generateSuggestion("alan@jeep.example", new AiSuggestionRequest(
            project.getId(),
            "We are late on the release and need a safer plan.",
            "risk_review"
        ));

        assertThat(response).isNotNull();
        assertThat(response.projectId()).isEqualTo(project.getId());
        assertThat(response.suggestion()).isNotBlank();
        assertThat(response.confidence()).isBetween(0.0, 1.0);
    }
}
