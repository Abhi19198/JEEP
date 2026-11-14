package com.jeep.notification.service;

import com.jeep.config.PasswordConfig;
import com.jeep.identity.domain.AccountStatus;
import com.jeep.identity.domain.User;
import com.jeep.identity.domain.UserRole;
import com.jeep.identity.exception.IdentityException;
import com.jeep.identity.repository.UserRepository;
import com.jeep.notification.dto.CreateNotificationRequest;
import com.jeep.notification.dto.NotificationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({NotificationService.class, PasswordConfig.class})
@ActiveProfiles("test")
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void createNotification_shouldPersistAndReturnNotification() {
        User user = userRepository.save(User.builder()
            .firstName("Mary")
            .lastName("Anning")
            .email("mary@jeep.example")
            .passwordHash(passwordEncoder.encode("StrongPass123!"))
            .status(AccountStatus.ACTIVE)
            .role(UserRole.VIEWER)
            .build());

        NotificationResponse response = notificationService.createNotification("mary@jeep.example", new CreateNotificationRequest(
            "TASK_UPDATE",
            "EMAIL",
            "Task status changed",
            "The task moved to review"
        ));

        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(response.title()).isEqualTo("Task status changed");
        assertThat(response.read()).isFalse();
    }

    @Test
    void createNotification_shouldFailForUnknownUser() {
        assertThatThrownBy(() -> notificationService.createNotification("ghost@jeep.example", new CreateNotificationRequest(
            "TASK_UPDATE",
            "EMAIL",
            "Task status changed",
            "The task moved to review"
        )))
            .isInstanceOf(IdentityException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    void createNotification_shouldFailForUnsupportedType() {
        userRepository.save(User.builder()
            .firstName("Mary")
            .lastName("Anning")
            .email("mary@jeep.example")
            .passwordHash(passwordEncoder.encode("StrongPass123!"))
            .status(AccountStatus.ACTIVE)
            .role(UserRole.VIEWER)
            .build());

        assertThatThrownBy(() -> notificationService.createNotification("mary@jeep.example", new CreateNotificationRequest(
            "BOGUS_TYPE",
            "EMAIL",
            "Task status changed",
            "The task moved to review"
        )))
            .isInstanceOf(IdentityException.class)
            .hasMessageContaining("Unsupported notification type");
    }

    @Test
    void markAsRead_shouldMarkOwnNotificationAsRead() {
        userRepository.save(User.builder()
            .firstName("Mary")
            .lastName("Anning")
            .email("mary@jeep.example")
            .passwordHash(passwordEncoder.encode("StrongPass123!"))
            .status(AccountStatus.ACTIVE)
            .role(UserRole.VIEWER)
            .build());

        NotificationResponse created = notificationService.createNotification("mary@jeep.example", new CreateNotificationRequest(
            "TASK_UPDATE",
            "IN_APP",
            "Task assigned",
            "You were assigned a task"
        ));
        assertThat(created.read()).isFalse();

        NotificationResponse marked = notificationService.markAsRead("mary@jeep.example", created.id());
        assertThat(marked.read()).isTrue();
        assertThat(marked.id()).isEqualTo(created.id());
    }

    @Test
    void markAsRead_shouldFailForUnknownNotification() {
        userRepository.save(User.builder()
            .firstName("Mary")
            .lastName("Anning")
            .email("mary@jeep.example")
            .passwordHash(passwordEncoder.encode("StrongPass123!"))
            .status(AccountStatus.ACTIVE)
            .role(UserRole.VIEWER)
            .build());

        assertThatThrownBy(() -> notificationService.markAsRead("mary@jeep.example", UUID.randomUUID()))
            .isInstanceOf(IdentityException.class)
            .hasMessageContaining("Notification not found");
    }

    @Test
    void markAsRead_shouldFailWhenNotificationBelongsToAnotherUser() {
        userRepository.save(User.builder()
            .firstName("Mary")
            .lastName("Anning")
            .email("mary@jeep.example")
            .passwordHash(passwordEncoder.encode("StrongPass123!"))
            .status(AccountStatus.ACTIVE)
            .role(UserRole.VIEWER)
            .build());
        userRepository.save(User.builder()
            .firstName("Alan")
            .lastName("Turing")
            .email("alan@jeep.example")
            .passwordHash(passwordEncoder.encode("StrongPass123!"))
            .status(AccountStatus.ACTIVE)
            .role(UserRole.VIEWER)
            .build());

        NotificationResponse created = notificationService.createNotification("mary@jeep.example", new CreateNotificationRequest(
            "TASK_UPDATE",
            "IN_APP",
            "Task assigned",
            "You were assigned a task"
        ));

        assertThatThrownBy(() -> notificationService.markAsRead("alan@jeep.example", created.id()))
            .isInstanceOf(IdentityException.class)
            .hasMessageContaining("does not belong");
    }
}
