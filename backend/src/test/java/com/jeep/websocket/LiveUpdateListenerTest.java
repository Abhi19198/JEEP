package com.jeep.websocket;

import com.jeep.notification.domain.ChannelType;
import com.jeep.notification.domain.NotificationType;
import com.jeep.notification.dto.NotificationResponse;
import com.jeep.task.domain.TaskPriority;
import com.jeep.task.domain.TaskStatus;
import com.jeep.task.dto.TaskResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LiveUpdateListenerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void onTaskChanged_shouldSendToProjectTopic() {
        UUID projectId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        TaskResponse task = new TaskResponse(taskId, projectId, null,
            "Fix login", "", TaskPriority.HIGH, TaskStatus.IN_PROGRESS, null, null);
        LiveUpdateListener listener = new LiveUpdateListener(messagingTemplate);

        listener.onTaskChanged(new TaskChangedEvent(projectId, UUID.randomUUID(),
            TaskChangeType.TASK_UPDATED, task));

        ArgumentCaptor<LiveTaskEvent> payload = ArgumentCaptor.forClass(LiveTaskEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/project/" + projectId), payload.capture());
        assertThat(payload.getValue().type()).isEqualTo("TASK_UPDATED");
        assertThat(payload.getValue().task()).isEqualTo(task);
    }

    @Test
    void onNotificationCreated_shouldSendToUserTopic() {
        UUID userId = UUID.randomUUID();
        NotificationResponse notification = new NotificationResponse(UUID.randomUUID(), userId,
            NotificationType.TASK_UPDATE, ChannelType.IN_APP,
            "Task assigned", "You were assigned a task", false, null);
        LiveUpdateListener listener = new LiveUpdateListener(messagingTemplate);

        listener.onNotificationCreated(new NotificationCreatedEvent(notification));

        verify(messagingTemplate).convertAndSend("/topic/user/" + userId + "/notifications", notification);
    }
}
