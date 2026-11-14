package com.jeep.notification.service;

import com.jeep.identity.domain.User;
import com.jeep.identity.exception.IdentityException;
import com.jeep.identity.repository.UserRepository;
import com.jeep.notification.domain.ChannelType;
import com.jeep.notification.domain.Notification;
import com.jeep.notification.domain.NotificationType;
import com.jeep.notification.dto.CreateNotificationRequest;
import com.jeep.notification.dto.NotificationResponse;
import com.jeep.notification.repository.NotificationRepository;
import com.jeep.websocket.NotificationCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public NotificationResponse createNotification(String userEmail, CreateNotificationRequest request) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IdentityException("User not found"));

        NotificationType type = switch (request.type().toUpperCase()) {
            case "TASK_UPDATE" -> NotificationType.TASK_UPDATE;
            case "COMMENT" -> NotificationType.COMMENT;
            case "MEMBER_ADDED" -> NotificationType.MEMBER_ADDED;
            case "PROJECT_UPDATE" -> NotificationType.PROJECT_UPDATE;
            case "SYSTEM" -> NotificationType.SYSTEM;
            default -> throw new IdentityException("Unsupported notification type");
        };

        ChannelType channel = switch (request.channel().toUpperCase()) {
            case "EMAIL" -> ChannelType.EMAIL;
            case "IN_APP" -> ChannelType.IN_APP;
            case "SLACK" -> ChannelType.SLACK;
            case "SMS" -> ChannelType.SMS;
            default -> throw new IdentityException("Unsupported notification channel");
        };

        Notification notification = Notification.builder()
            .user(user)
            .type(type)
            .channel(channel)
            .title(request.title())
            .message(request.message())
            .build();

        Notification saved = notificationRepository.save(notification);
        NotificationResponse response = toResponse(saved);
        eventPublisher.publishEvent(new NotificationCreatedEvent(response));
        return response;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IdentityException("User not found"));

        return notificationRepository.findByUserOrderByCreatedAtDesc(user).stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Creates a notification for a specific user (used for automatic
     * notifications, e.g. when a task is assigned or its status changes).
     * Unlike {@link #createNotification}, the recipient is given directly
     * instead of being the authenticated caller.
     */
    @Transactional
    public NotificationResponse createForUser(User recipient, NotificationType type, ChannelType channel, String title, String message) {
        Notification notification = Notification.builder()
            .user(recipient)
            .type(type)
            .channel(channel)
            .title(title)
            .message(message)
            .build();

        NotificationResponse response = toResponse(notificationRepository.save(notification));
        eventPublisher.publishEvent(new NotificationCreatedEvent(response));
        return response;
    }

    @Transactional
    public NotificationResponse markAsRead(String userEmail, UUID notificationId) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IdentityException("User not found"));
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IdentityException("Notification not found"));
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new IdentityException("Notification does not belong to this user");
        }
        notification.markAsRead();
        return toResponse(notification);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
            notification.getId(),
            notification.getUser().getId(),
            notification.getType(),
            notification.getChannel(),
            notification.getTitle(),
            notification.getMessage(),
            notification.isRead(),
            notification.getCreatedAt()
        );
    }
}
