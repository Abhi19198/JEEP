package com.jeep.organization.repository;

import com.jeep.identity.domain.User;
import com.jeep.organization.domain.Organization;
import com.jeep.organization.domain.OrganizationMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {

    Optional<OrganizationMember> findByOrganizationAndUser(Organization organization, User user);
}
