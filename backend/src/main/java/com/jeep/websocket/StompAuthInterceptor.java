package com.jeep.websocket;

import com.jeep.identity.domain.User;
import com.jeep.identity.repository.UserRepository;
import com.jeep.identity.service.TokenService;
import com.jeep.organization.repository.OrganizationMemberRepository;
import com.jeep.project.domain.Project;
import com.jeep.project.repository.ProjectRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Authenticates STOMP connections and authorizes topic subscriptions.
 *
 * Browsers cannot set HTTP headers on a WebSocket handshake, so the JWT
 * arrives as an {@code Authorization: Bearer ...} header of the STOMP
 * CONNECT frame instead. Refresh tokens are rejected here; only access
 * tokens may open a socket. Subscriptions are then checked against the
 * database: a project topic requires organization membership, and a
 * user's notification topic may only be subscribed to by that user.
 * Anything else is rejected and the client receives a STOMP ERROR frame.
 */
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // getAccessor (not StompHeaderAccessor.wrap): it returns the
        // decoder-attached accessor whose header map is shared with the
        // message, so setUser() below actually reaches downstream
        // processing. wrap() would copy the headers and lose the principal.
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }
        return message;
    }

    private void authenticateConnect(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Missing Authorization header in STOMP CONNECT");
        }
        Claims claims;
        try {
            claims = tokenService.parseToken(authorization.substring(7).trim());
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid token in STOMP CONNECT");
        }
        if ("refresh".equals(claims.get("tokenType", String.class))) {
            throw new IllegalArgumentException("Refresh token cannot be used for WebSocket");
        }
        String email = claims.getSubject();
        String role = claims.get("role", String.class);
        if (email == null || role == null) {
            throw new IllegalArgumentException("Invalid token in STOMP CONNECT");
        }
        accessor.setUser(new UsernamePasswordAuthenticationToken(
            email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }
        if (accessor.getUser() == null) {
            throw new IllegalArgumentException("Not authenticated");
        }
        String email = accessor.getUser().getName();
        if (destination.startsWith("/topic/project/")) {
            UUID projectId = parseUuid(destination.substring("/topic/project/".length()), "project");
            Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
            organizationMemberRepository.findByOrganizationAndUser(project.getOrganization(), user)
                .orElseThrow(() -> new IllegalArgumentException("User is not a member of this organization"));
        } else if (destination.startsWith("/topic/user/") && destination.endsWith("/notifications")) {
            String userId = destination.substring(
                "/topic/user/".length(), destination.length() - "/notifications".length());
            parseUuid(userId, "user");
            User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (!user.getId().toString().equals(userId)) {
                throw new IllegalArgumentException("Cannot subscribe to another user's notifications");
            }
        } else {
            throw new IllegalArgumentException("Unknown destination: " + destination);
        }
    }

    private UUID parseUuid(String value, String what) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid " + what + " id in destination");
        }
    }
}
