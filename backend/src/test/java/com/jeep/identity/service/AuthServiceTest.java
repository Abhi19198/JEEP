package com.jeep.identity.service;

import com.jeep.config.PasswordConfig;
import com.jeep.identity.dto.AuthResponse;
import com.jeep.identity.dto.LoginRequest;
import com.jeep.identity.dto.RegisterRequest;
import com.jeep.identity.repository.UserRepository;
import com.jeep.identity.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({AuthService.class, PasswordConfig.class, TokenService.class})
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void register_shouldCreateUserAndReturnToken() {
        RegisterRequest request = new RegisterRequest(
            "ada",
            "lovelace",
            "ada@jeep.example",
            "StrongPass123!",
            "StrongPass123!"
        );

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(userRepository.findByEmail("ada@jeep.example")).isPresent();
        assertThat(passwordEncoder.matches("StrongPass123!", userRepository.findByEmail("ada@jeep.example").orElseThrow().getPasswordHash())).isTrue();
    }

    @Test
    void login_shouldAuthenticateWithValidCredentials() {
        RegisterRequest request = new RegisterRequest(
            "grace",
            "hopper",
            "grace@jeep.example",
            "StrongPass123!",
            "StrongPass123!"
        );

        authService.register(request);

        AuthResponse response = authService.login(new LoginRequest("grace@jeep.example", "StrongPass123!"));

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
    }
}
