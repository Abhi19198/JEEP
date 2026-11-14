package com.jeep.project.repository;

import com.jeep.organization.domain.Organization;
import com.jeep.project.domain.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByOrganization(Organization organization);

    long countByOrganization(Organization organization);
}
