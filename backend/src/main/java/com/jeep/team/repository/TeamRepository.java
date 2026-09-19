package com.jeep.team.repository;

import com.jeep.organization.domain.Organization;
import com.jeep.team.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {

    List<Team> findByOrganization(Organization organization);
}
