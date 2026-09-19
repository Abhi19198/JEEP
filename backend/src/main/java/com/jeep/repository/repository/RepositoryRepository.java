package com.jeep.repository.repository;

import com.jeep.project.domain.Project;
import com.jeep.repository.domain.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RepositoryRepository extends JpaRepository<Repository, UUID> {

    List<Repository> findByProject(Project project);
}
