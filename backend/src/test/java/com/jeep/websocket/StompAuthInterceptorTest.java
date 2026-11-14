package com.jeep.websocket;

import com.jeep.identity.domain.User;
import com.jeep.identity.repository.UserRepository;
import com.jeep.identity.service.TokenService;
import com.jeep.organization.domain.Organization;
import com.jeep.organization.domain.OrganizationMember;
import com.jeep.organization.repository.OrganizationMemberRepository;
import com.jeep.project.domain.Project;
import com.jeep.project.repository.ProjectRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Verifies STOMP CONNECT authentication and SUBSCRIBE authorization.
 * Messages are built the way the STOMP decoder builds them (via the
 * accessor's own message headers), so the interceptor sees the same
 * attached accessor as in production.
 */
@ExtendWith(MockitoExtension.class)
class StompAuthInterceptorTest {

    @Mock private TokenService tokenService;
    @Mock private UserRepository userRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private OrganizationMemberRepository organizationMemberRepository;

    private StompAuthInterceptor interceptor() {
        return new StompAuthInterceptor(tokenService, userRepository,
            projectRepository, organizationMemberRepository);
    }

    private Message<byte[]> connectMessage(String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        // Mirror StompDecoder: leave headers mutable so the interceptor
        // can set the authenticated principal on the attached accessor.
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<byte[]> subscribeMessage(String destination, String email) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(new UsernamePasswordAuthenticationToken(email, null, java.util.List.of()));
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Claims claimsFor(String email, String role, String tokenType) {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(email);
        when(claims.get("role", String.class)).thenReturn(role);
        when(claims.get("tokenType", String.class)).thenReturn(tokenType);
        return claims;
    }

    @Test
    void connect_withValidAccessToken_shouldSetPrincipal() {
        Claims claims = claimsFor("ada@jeep.example", "ADMIN", null);
        when(tokenService.parseToken("good-token")).thenReturn(claims);
        Message<byte[]> message = connectMessage("good-token");

        Message<?> result = interceptor().preSend(message, null);

        StompHeaderAccessor accessor =
            org.springframework.messaging.support.MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(accessor).isNotNull();
        assertThat(accessor.getUser()).isNotNull();
        assertThat(accessor.getUser().getName()).isEqualTo("ada@jeep.example");
    }

    @Test
    void connect_withRefreshToken_shouldReject() {
        // Only tokenType is stubbed: the interceptor rejects a refresh
        // token before it ever reads the subject or role.
        Claims claims = mock(Claims.class);
        when(claims.get("tokenType", String.class)).thenReturn("refresh");
        when(tokenService.parseToken("refresh-token")).thenReturn(claims);

        assertThatThrownBy(() -> interceptor().preSend(connectMessage("refresh-token"), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Refresh token");
    }

    @Test
    void connect_withInvalidToken_shouldReject() {
        when(tokenService.parseToken("bad-token")).thenThrow(new JwtException("bad"));

        assertThatThrownBy(() -> interceptor().preSend(connectMessage("bad-token"), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid token");
    }

    @Test
    void connect_withoutAuthorizationHeader_shouldReject() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThatThrownBy(() -> interceptor().preSend(message, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Missing Authorization");
    }

    @Test
    void subscribe_toProjectTopic_asMember_shouldAllow() {
        UUID projectId = UUID.randomUUID();
        User user = mock(User.class);
        Project project = mock(Project.class);
        Organization org = mock(Organization.class);
        when(project.getOrganization()).thenReturn(org);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("ada@jeep.example")).thenReturn(Optional.of(user));
        when(organizationMemberRepository.findByOrganizationAndUser(org, user))
            .thenReturn(Optional.of(mock(OrganizationMember.class)));

        Message<?> result = interceptor().preSend(
            subscribeMessage("/topic/project/" + projectId, "ada@jeep.example"), null);

        assertThat(result).isNotNull();
    }

    @Test
    void subscribe_toProjectTopic_asNonMember_shouldReject() {
        UUID projectId = UUID.randomUUID();
        User user = mock(User.class);
        Project project = mock(Project.class);
        Organization org = mock(Organization.class);
        when(project.getOrganization()).thenReturn(org);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(userRepository.findByEmail("grace@jeep.example")).thenReturn(Optional.of(user));
        when(organizationMemberRepository.findByOrganizationAndUser(org, user))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> interceptor().preSend(
                subscribeMessage("/topic/project/" + projectId, "grace@jeep.example"), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not a member");
    }

    @Test
    void subscribe_toOwnNotificationTopic_shouldAllow() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(userRepository.findByEmail("ada@jeep.example")).thenReturn(Optional.of(user));

        Message<?> result = interceptor().preSend(
            subscribeMessage("/topic/user/" + userId + "/notifications", "ada@jeep.example"), null);

        assertThat(result).isNotNull();
    }

    @Test
    void subscribe_toAnotherUsersNotificationTopic_shouldReject() {
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(userRepository.findByEmail("ada@jeep.example")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> interceptor().preSend(
                subscribeMessage("/topic/user/" + UUID.randomUUID() + "/notifications", "ada@jeep.example"), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("another user");
    }

    @Test
    void subscribe_toUnknownDestination_shouldReject() {
        assertThatThrownBy(() -> interceptor().preSend(
                subscribeMessage("/topic/admin/broadcast", "ada@jeep.example"), null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown destination");
    }
}
