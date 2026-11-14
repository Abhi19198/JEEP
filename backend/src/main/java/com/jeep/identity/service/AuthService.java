package com.jeep.identity.service;

import com.jeep.identity.domain.AccountStatus;
import com.jeep.identity.domain.User;
import com.jeep.identity.domain.UserRole;
import com.jeep.identity.dto.AuthResponse;
import com.jeep.identity.dto.LoginRequest;
import com.jeep.identity.dto.RefreshTokenRequest;
import com.jeep.identity.dto.RegisterRequest;
import com.jeep.identity.exception.DuplicateUserException;
import com.jeep.identity.exception.IdentityException;
import com.jeep.identity.exception.InvalidCredentialsException;
import com.jeep.identity.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!request.passwordsMatch()) {
            throw new IdentityException("Password and confirmation do not match");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateUserException(request.email());
        }

        User user = User.builder()
            .firstName(request.firstName())
            .lastName(request.lastName())
            .email(request.email().trim())
            .passwordHash(passwordEncoder.encode(request.password()))
            .status(AccountStatus.ACTIVE)
            .role(UserRole.VIEWER)
            .build();

        User savedUser = userRepository.save(user);

        String accessToken = tokenService.generateAccessToken(savedUser);
        String refreshToken = tokenService.generateRefreshToken(savedUser);

        return new AuthResponse(accessToken, refreshToken, "Bearer", savedUser.getEmail(), savedUser.getRole().name(), savedUser.getId());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(InvalidCredentialsException::new);

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new IdentityException("Account is not active");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        user.recordLogin();

        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, "Bearer", user.getEmail(), user.getRole().name(), user.getId());
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        Claims claims;
        try {
            claims = tokenService.parseToken(request.refreshToken());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidCredentialsException();
        }

        if (!"refresh".equals(claims.get("tokenType", String.class))) {
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmail(claims.getSubject())
            .orElseThrow(InvalidCredentialsException::new);

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new IdentityException("Account is not active");
        }

        String accessToken = tokenService.generateAccessToken(user);

        return new AuthResponse(accessToken, request.refreshToken(), "Bearer", user.getEmail(), user.getRole().name(), user.getId());
    }
}
