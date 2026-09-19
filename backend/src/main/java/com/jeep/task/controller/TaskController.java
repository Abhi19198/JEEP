package com.jeep.task.controller;

import com.jeep.task.dto.CreateTaskRequest;
import com.jeep.task.dto.TaskResponse;
import com.jeep.task.dto.UpdateTaskRequest;
import com.jeep.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
        Authentication authentication,
        @Valid @RequestBody CreateTaskRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(taskService.createTask(authentication.getName(), request));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<TaskResponse>> getTasksForProject(Authentication authentication, @PathVariable UUID projectId) {
        return ResponseEntity.ok(taskService.getTasksForProject(authentication.getName(), projectId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTask(Authentication authentication, @PathVariable UUID id) {
        return ResponseEntity.ok(taskService.getTask(authentication.getName(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
        Authentication authentication,
        @PathVariable UUID id,
        @Valid @RequestBody UpdateTaskRequest request
    ) {
        return ResponseEntity.ok(taskService.updateTask(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(Authentication authentication, @PathVariable UUID id) {
        taskService.deleteTask(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
