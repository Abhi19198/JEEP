package com.jeep.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over plain WebSocket (no SockJS) at {@code /ws}.
 *
 * Topics:
 * <ul>
 *   <li>{@code /topic/project/{projectId}} — task created/updated/deleted in that project</li>
 *   <li>{@code /topic/user/{userId}/notifications} — notifications created for that user</li>
 * </ul>
 * The HTTP handshake itself is permitted in {@code SecurityConfig};
 * authentication and per-topic authorization happen in
 * {@link StompAuthInterceptor} on CONNECT/SUBSCRIBE, because browsers
 * cannot send an Authorization header with the handshake. All REST
 * endpoints keep working unchanged as the fallback.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthInterceptor stompAuthInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthInterceptor);
    }
}
