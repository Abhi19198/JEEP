package com.jeep.task.service;

import com.jeep.identity.domain.User;
import com.jeep.identity.exception.IdentityException;
import com.jeep.identity.repository.UserRepository;
import com.jeep.notification.domain.ChannelType;
import com.jeep.notification.domain.NotificationType;
import com.jeep.notification.service.NotificationService;
import com.jeep.project.domain.Project;
import com.jeep.project.repository.ProjectRepository;
import com.jeep.organization.repository.OrganizationMemberRepository;
import com.jeep.task.domain.Task;
import com.jeep.task.domain.TaskPriority;
import com.jeep.task.domain.TaskStatus;
import com.jeep.task.dto.CreateTaskRequest;
import com.jeep.task.dto.TaskResponse;
import com.jeep.task.dto.UpdateTaskRequest;
import com.jeep.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public TaskResponse createTask(String userEmail, CreateTaskRequest request) {
        User actor = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IdentityException("User not found"));

        Project project = projectRepository.findById(request.projectId())
            .orElseThrow(() -> new IdentityException("Project not found"));
        organizationMemberRepository.findByOrganizationAndUser(project.getOrganization(), actor)
            .orElseThrow(() -> new IdentityException("User is not a member of this organization"));

        User assignee = request.assigneeId() == null ? actor : userRepository.findById(request.assigneeId())
            .orElseThrow(() -> new IdentityException("Assignee not found"));

        Task task = Task.builder()
            .project(project)
            .assignee(assignee)
            .title(request.title())
            .description(request.description())
            .priority(request.priority() == null ? TaskPriority.MEDIUM : request.priority())
            .status(request.status() == null ? TaskStatus.TODO : request.status())
            .build();

        Task saved = taskRepository.save(task);
        // Auto-notification: tell the assignee they have a new task, unless
        // they assigned it to themselves.
        if (saved.getAssignee() != null && !saved.getAssignee().getId().equals(actor.getId())) {
            notificationService.createForUser(
                saved.getAssignee(),
                NotificationType.TASK_UPDATE,
                ChannelType.IN_APP,
                "Task assigned: " + saved.getTitle(),
                "You were assigned task '" + saved.getTitle() + "' in project '" + project.getName() + "'."
            );
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> getTasksForProject(String userEmail, UUID projectId, String search, Pageable pageable) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IdentityException("User not found"));
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new IdentityException("Project not found"));
        organizationMemberRepository.findByOrganizationAndUser(project.getOrganization(), user)
            .orElseThrow(() -> new IdentityException("User is not a member of this organization"));

        Page<Task> page = (search == null || search.isBlank())
            ? taskRepository.findByProject(project, pageable)
            : taskRepository.searchByProject(project, search.trim(), pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(String userEmail, UUID taskId) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IdentityException("User not found"));
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IdentityException("Task not found"));
        organizationMemberRepository.findByOrganizationAndUser(task.getProject().getOrganization(), user)
            .orElseThrow(() -> new IdentityException("User is not a member of this organization"));
        return toResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(String userEmail, UUID taskId, UpdateTaskRequest request) {
        Task task = findMemberTask(userEmail, taskId);
        TaskStatus previousStatus = task.getStatus();
        User previousAssignee = task.getAssignee();
        User assignee = request.assigneeId() == null ? task.getAssignee() : userRepository.findById(request.assigneeId())
            .orElseThrow(() -> new IdentityException("Assignee not found"));
        task.update(request.title(), request.description(), request.priority(), request.status(), assignee);
        // Auto-notifications: a new assignee is told about the assignment;
        // otherwise the (unchanged) assignee is told about a status change.
        boolean assigneeChanged = assignee != null
            && (previousAssignee == null || !previousAssignee.getId().equals(assignee.getId()));
        if (assigneeChanged) {
            notificationService.createForUser(
                assignee,
                NotificationType.TASK_UPDATE,
                ChannelType.IN_APP,
                "Task assigned: " + task.getTitle(),
                "You were assigned task '" + task.getTitle() + "' in project '" + task.getProject().getName() + "'."
            );
        } else if (request.status() != null && !request.status().equals(previousStatus) && assignee != null) {
            notificationService.createForUser(
                assignee,
                NotificationType.TASK_UPDATE,
                ChannelType.IN_APP,
                "Task status changed: " + task.getTitle(),
                "Task '" + task.getTitle() + "' moved from " + previousStatus + " to " + request.status() + "."
            );
        }
        return toResponse(task);
    }

    @Transactional
    public void deleteTask(String userEmail, UUID taskId) {
        taskRepository.delete(findMemberTask(userEmail, taskId));
    }

    private Task findMemberTask(String userEmail, UUID taskId) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IdentityException("User not found"));
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IdentityException("Task not found"));
        organizationMemberRepository.findByOrganizationAndUser(task.getProject().getOrganization(), user)
            .orElseThrow(() -> new IdentityException("User is not a member of this organization"));
        return task;
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
            task.getId(),
            task.getProject().getId(),
            task.getAssignee() != null ? task.getAssignee().getId() : null,
            task.getTitle(),
            task.getDescription(),
            task.getPriority(),
            task.getStatus(),
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }
}
