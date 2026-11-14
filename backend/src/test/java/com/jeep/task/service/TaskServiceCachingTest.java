package com.jeep.task.service;

import com.jeep.identity.domain.User;
import com.jeep.identity.repository.UserRepository;
import com.jeep.notification.service.NotificationService;
import com.jeep.organization.domain.Organization;
import com.jeep.organization.domain.OrganizationMember;
import com.jeep.organization.repository.OrganizationMemberRepository;
import com.jeep.project.domain.Project;
import com.jeep.project.repository.ProjectRepository;
import com.jeep.shared.cache.CacheService;
import com.jeep.task.domain.Task;
import com.jeep.task.domain.TaskPriority;
import com.jeep.task.domain.TaskStatus;
import com.jeep.task.dto.CreateTaskRequest;
import com.jeep.task.dto.TaskPageSnapshot;
import com.jeep.task.dto.TaskResponse;
import com.jeep.task.repository.TaskRepository;
import com.jeep.websocket.TaskChangeType;
import com.jeep.websocket.TaskChangedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the cache-aside behaviour and live-update events of
 * {@link TaskService} with mocked collaborators. The database-backed
 * slice test still covers the queries themselves.
 */
@ExtendWith(MockitoExtension.class)
// Lenient: the shared fixture stubs every lookup, but each test only
// exercises one code path (reads never evict, delete never looks the
// project up by id); the verify() assertions below stay strict.
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskServiceCachingTest {

    @Mock private TaskRepository taskRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private OrganizationMemberRepository organizationMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private CacheService cacheService;

    private TaskService taskService() {
        return new TaskService(taskRepository, projectRepository, organizationMemberRepository,
            userRepository, notificationService, eventPublisher, cacheService);
    }

    private record Fixture(UUID orgId, UUID projectId, UUID userId, User user, Organization org, Project project) {}

    private Fixture fixture() {
        UUID orgId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);
        Organization org = mock(Organization.class);
        when(org.getId()).thenReturn(orgId);
        Project project = mock(Project.class);
        when(project.getId()).thenReturn(projectId);
        when(project.getOrganization()).thenReturn(org);
        when(userRepository.findByEmail("grace@jeep.example")).thenReturn(Optional.of(user));
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(organizationMemberRepository.findByOrganizationAndUser(org, user))
            .thenReturn(Optional.of(mock(OrganizationMember.class)));
        return new Fixture(orgId, projectId, userId, user, org, project);
    }

    @Test
    void getTasksForProject_shouldCacheMissThenStore() {
        Fixture f = fixture();
        Pageable pageable = PageRequest.of(0, 12);
        Task task = Task.builder().project(f.project()).assignee(f.user()).title("Fix login")
            .description("").priority(TaskPriority.HIGH).status(TaskStatus.TODO).build();
        when(cacheService.get(any(), eq(TaskPageSnapshot.class))).thenReturn(Optional.empty());
        when(taskRepository.findByProject(f.project(), pageable))
            .thenReturn(new PageImpl<>(List.of(task), pageable, 1));

        Page<TaskResponse> result = taskService().getTasksForProject("grace@jeep.example", f.projectId(), null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Fix login");
        ArgumentCaptor<TaskPageSnapshot> snapshot = ArgumentCaptor.forClass(TaskPageSnapshot.class);
        verify(cacheService).put(any(), snapshot.capture(), eq(CacheService.TASK_LIST_TTL));
        assertThat(snapshot.getValue().content()).hasSize(1);
        assertThat(snapshot.getValue().totalElements()).isEqualTo(1);
    }

    @Test
    void getTasksForProject_shouldServeFromCacheWithoutQuerying() {
        Fixture f = fixture();
        Pageable pageable = PageRequest.of(0, 12);
        TaskResponse cachedTask = new TaskResponse(UUID.randomUUID(), f.projectId(), f.userId(),
            "Cached task", "", TaskPriority.MEDIUM, TaskStatus.DONE, null, null);
        when(cacheService.get(any(), eq(TaskPageSnapshot.class))).thenReturn(Optional.of(
            new TaskPageSnapshot(List.of(cachedTask), 1, 1, 0, 12)));

        Page<TaskResponse> result = taskService().getTasksForProject("grace@jeep.example", f.projectId(), null, pageable);

        assertThat(result.getContent()).containsExactly(cachedTask);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(taskRepository, never()).findByProject(any(), any());
        verify(taskRepository, never()).searchByProject(any(), any(), any());
    }

    @Test
    void createTask_shouldEvictCachesAndPublishEvent() {
        Fixture f = fixture();
        when(f.user().getId()).thenReturn(f.userId());
        Task task = Task.builder().project(f.project()).assignee(f.user()).title("New task")
            .description("").priority(TaskPriority.MEDIUM).status(TaskStatus.TODO).build();
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        TaskResponse response = taskService().createTask("grace@jeep.example",
            new CreateTaskRequest(f.projectId(), "New task", "", TaskPriority.MEDIUM, null, TaskStatus.TODO));

        assertThat(response.title()).isEqualTo("New task");
        verify(cacheService).evictByPrefix("tasks:project:" + f.projectId() + ":");
        verify(cacheService).evict("analytics:org:" + f.orgId());
        ArgumentCaptor<TaskChangedEvent> event = ArgumentCaptor.forClass(TaskChangedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().type()).isEqualTo(TaskChangeType.TASK_CREATED);
        assertThat(event.getValue().projectId()).isEqualTo(f.projectId());
    }

    @Test
    void deleteTask_shouldEvictCachesAndPublishDeletedEvent() {
        Fixture f = fixture();
        when(f.user().getId()).thenReturn(f.userId());
        UUID taskId = UUID.randomUUID();
        Task task = Task.builder().project(f.project()).assignee(f.user()).title("Old task")
            .description("").priority(TaskPriority.LOW).status(TaskStatus.DONE).build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(task));

        taskService().deleteTask("grace@jeep.example", taskId);

        verify(taskRepository).delete(task);
        verify(cacheService).evictByPrefix("tasks:project:" + f.projectId() + ":");
        verify(cacheService).evict("analytics:org:" + f.orgId());
        ArgumentCaptor<TaskChangedEvent> event = ArgumentCaptor.forClass(TaskChangedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().type()).isEqualTo(TaskChangeType.TASK_DELETED);
        assertThat(event.getValue().task().title()).isEqualTo("Old task");
    }
}
