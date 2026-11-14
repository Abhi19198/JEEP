package com.jeep.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Forwards domain events to STOMP topics. Plain {@code @EventListener}
 * (not after-commit): the payloads are self-contained, so a subscriber
 * never needs to re-read the database, and publishing works the same
 * whether or not the publisher runs inside a transaction. The existing
 * REST endpoints stay untouched and remain the fallback when a client
 * is not connected.
 */
@Component
@RequiredArgsConstructor
public class LiveUpdateListener {

    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void onTaskChanged(TaskChangedEvent event) {
        messagingTemplate.convertAndSend(
            "/topic/project/" + event.projectId(),
            new LiveTaskEvent(event.type().name(), event.task()));
    }

    @EventListener
    public void onNotificationCreated(NotificationCreatedEvent event) {
        messagingTemplate.convertAndSend(
            "/topic/user/" + event.notification().userId() + "/notifications",
            event.notification());
    }
}
