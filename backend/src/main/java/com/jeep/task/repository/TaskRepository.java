package com.jeep.task.repository;

import com.jeep.organization.domain.Organization;
import com.jeep.project.domain.Project;
import com.jeep.task.domain.Task;
import com.jeep.task.domain.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByProject(Project project);

    long countByProject_Organization(Organization organization);

    long countByProject_OrganizationAndStatus(Organization organization, TaskStatus status);

    long countByProject_OrganizationAndStatusIn(Organization organization, Collection<TaskStatus> statuses);

    long countByProject_OrganizationAndStatusNotIn(Organization organization, Collection<TaskStatus> statuses);
}
