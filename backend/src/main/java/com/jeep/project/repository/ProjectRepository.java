package com.jeep.project.repository;

import com.jeep.organization.domain.Organization;
import com.jeep.project.domain.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByOrganization(Organization organization);

    Page<Project> findByOrganization(Organization organization, Pageable pageable);

    @Query("select p from Project p where p.organization = :organization and "
        + "(lower(p.name) like lower(concat('%', :search, '%')) "
        + "or lower(coalesce(p.description, '')) like lower(concat('%', :search, '%')))")
    Page<Project> searchByOrganization(@Param("organization") Organization organization, @Param("search") String search, Pageable pageable);

    long countByOrganization(Organization organization);
}
