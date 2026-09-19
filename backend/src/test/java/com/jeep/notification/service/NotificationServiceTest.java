package com.jeep.notification.service;

import com.jeep.config.PasswordConfig;
import com.jeep.identity.domain.AccountStatus;
import com.jeep.identity.domain.User;
import com.jeep.identity.domain.UserRole;
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

import static org.assertj.core.api.Assertions.assertThat;

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
}
