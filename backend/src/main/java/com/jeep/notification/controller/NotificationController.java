package com.jeep.notification.controller;

import com.jeep.notification.dto.CreateNotificationRequest;
import com.jeep.notification.dto.NotificationResponse;
import com.jeep.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(
        Authentication authentication,
        @Valid @RequestBody CreateNotificationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(notificationService.createNotification(authentication.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(Authentication authentication) {
        return ResponseEntity.ok(notificationService.getNotificationsForUser(authentication.getName()));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
        Authentication authentication,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(notificationService.markAsRead(authentication.getName(), id));
    }
}
