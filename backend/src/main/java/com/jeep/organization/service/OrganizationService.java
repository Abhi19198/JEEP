package com.jeep.organization.service;

import com.jeep.identity.domain.User;
import com.jeep.identity.exception.IdentityException;
import com.jeep.identity.repository.UserRepository;
import com.jeep.organization.domain.Organization;
import com.jeep.organization.domain.OrganizationMember;
import com.jeep.organization.domain.OrganizationRole;
import com.jeep.organization.dto.CreateOrganizationRequest;
import com.jeep.organization.dto.OrganizationResponse;
import com.jeep.organization.repository.OrganizationMemberRepository;
import com.jeep.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public OrganizationResponse createOrganization(String userEmail, CreateOrganizationRequest request) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IdentityException("User not found"));

        Organization organization = Organization.builder()
            .name(request.name())
            .description(request.description())
            .active(true)
            .build();

        Organization saved = organizationRepository.save(organization);

        OrganizationMember member = OrganizationMember.builder()
            .organization(saved)
            .user(user)
            .role(OrganizationRole.OWNER)
            .build();

        organizationMemberRepository.save(member);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> getOrganizationsForUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IdentityException("User not found"));

        return organizationMemberRepository.findAll().stream()
            .filter(member -> member.getUser().getId().equals(user.getId()))
            .map(member -> toResponse(member.getOrganization()))
            .toList();
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getOrganization(String userEmail, UUID organizationId) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IdentityException("User not found"));
        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new IdentityException("Organization not found"));
        organizationMemberRepository.findByOrganizationAndUser(organization, user)
            .orElseThrow(() -> new IdentityException("User is not a member of this organization"));
        return toResponse(organization);
    }

    @Transactional
    public OrganizationResponse updateOrganization(String userEmail, UUID organizationId, CreateOrganizationRequest request) {
        Organization organization = findMemberOrganization(userEmail, organizationId);
        organization.update(request.name(), request.description());
        return toResponse(organization);
    }

    @Transactional
    public void deactivateOrganization(String userEmail, UUID organizationId) {
        findMemberOrganization(userEmail, organizationId).deactivate();
    }

    private Organization findMemberOrganization(String userEmail, UUID organizationId) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IdentityException("User not found"));
        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new IdentityException("Organization not found"));
        organizationMemberRepository.findByOrganizationAndUser(organization, user)
            .orElseThrow(() -> new IdentityException("User is not a member of this organization"));
        return organization;
    }

    private OrganizationResponse toResponse(Organization organization) {
        return new OrganizationResponse(
            organization.getId(),
            organization.getName(),
            organization.getDescription(),
            organization.isActive(),
            organization.getCreatedAt(),
            organization.getUpdatedAt()
        );
    }
}
