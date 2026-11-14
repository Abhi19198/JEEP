package com.jeep.identity.service;

import com.jeep.config.PasswordConfig;
import com.jeep.identity.dto.AuthResponse;
import com.jeep.identity.dto.LoginRequest;
import com.jeep.identity.dto.RefreshTokenRequest;
import com.jeep.identity.dto.RegisterRequest;
import com.jeep.identity.exception.DuplicateUserException;
import com.jeep.identity.exception.IdentityException;
import com.jeep.identity.exception.InvalidCredentialsException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void login_shouldFailWithWrongPassword() {
        authService.register(new RegisterRequest(
            "grace",
            "hopper",
            "grace@jeep.example",
            "StrongPass123!",
            "StrongPass123!"
        ));

        assertThatThrownBy(() -> authService.login(new LoginRequest("grace@jeep.example", "WrongPass123!")))
            .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_shouldFailForUnknownEmail() {
        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@jeep.example", "StrongPass123!")))
            .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void register_shouldFailWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest(
            "ada",
            "lovelace",
            "ada@jeep.example",
            "StrongPass123!",
            "StrongPass123!"
        );

        authService.register(request);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(DuplicateUserException.class);
    }

    @Test
    void register_shouldFailWhenPasswordsDoNotMatch() {
        RegisterRequest request = new RegisterRequest(
            "ada",
            "lovelace",
            "mismatch@jeep.example",
            "StrongPass123!",
            "DifferentPass123!"
        );

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(IdentityException.class)
            .hasMessageContaining("do not match");
    }

    @Test
    void refresh_shouldReturnNewAccessTokenForValidRefreshToken() {
        authService.register(new RegisterRequest(
            "grace",
            "hopper",
            "grace@jeep.example",
            "StrongPass123!",
            "StrongPass123!"
        ));
        AuthResponse login = authService.login(new LoginRequest("grace@jeep.example", "StrongPass123!"));

        AuthResponse refreshed = authService.refresh(new RefreshTokenRequest(login.refreshToken()));

        assertThat(refreshed).isNotNull();
        assertThat(refreshed.accessToken()).isNotBlank();
        assertThat(refreshed.refreshToken()).isEqualTo(login.refreshToken());
        assertThat(refreshed.email()).isEqualTo("grace@jeep.example");
    }

    @Test
    void refresh_shouldRejectAccessToken() {
        authService.register(new RegisterRequest(
            "grace",
            "hopper",
            "grace@jeep.example",
            "StrongPass123!",
            "StrongPass123!"
        ));
        AuthResponse login = authService.login(new LoginRequest("grace@jeep.example", "StrongPass123!"));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest(login.accessToken())))
            .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refresh_shouldRejectMalformedToken() {
        assertThatThrownBy(() -> authService.refresh(new RefreshTokenRequest("not-a-token")))
            .isInstanceOf(InvalidCredentialsException.class);
    }
}
