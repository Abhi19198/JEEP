package com.jeep.task.repository;

import com.jeep.organization.domain.Organization;
import com.jeep.project.domain.Project;
import com.jeep.task.domain.Task;
import com.jeep.task.domain.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByProject(Project project);

    Page<Task> findByProject(Project project, Pageable pageable);

    @Query("select t from Task t where t.project = :project and "
        + "(lower(t.title) like lower(concat('%', :search, '%')) "
        + "or lower(coalesce(t.description, '')) like lower(concat('%', :search, '%')))")
    Page<Task> searchByProject(@Param("project") Project project, @Param("search") String search, Pageable pageable);

    long countByProject_Organization(Organization organization);

    long countByProject_OrganizationAndStatus(Organization organization, TaskStatus status);

    long countByProject_OrganizationAndStatusIn(Organization organization, Collection<TaskStatus> statuses);

    long countByProject_OrganizationAndStatusNotIn(Organization organization, Collection<TaskStatus> statuses);
}
