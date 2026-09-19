package com.jeep.team.service;

import com.jeep.identity.domain.User;
import com.jeep.identity.exception.IdentityException;
import com.jeep.identity.repository.UserRepository;
import com.jeep.organization.domain.Organization;
import com.jeep.organization.domain.OrganizationMember;
import com.jeep.organization.repository.OrganizationMemberRepository;
import com.jeep.organization.repository.OrganizationRepository;
import com.jeep.team.domain.Team;
import com.jeep.team.domain.TeamMember;
import com.jeep.team.domain.TeamRole;
import com.jeep.team.dto.CreateTeamRequest;
import com.jeep.team.dto.TeamResponse;
import com.jeep.team.repository.TeamMemberRepository;
import com.jeep.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public TeamResponse createTeam(String userEmail, CreateTeamRequest request) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IdentityException("User not found"));

        Organization organization = organizationRepository.findById(UUID.fromString(request.organizationId()))
            .orElseThrow(() -> new IdentityException("Organization not found"));

        OrganizationMember member = organizationMemberRepository.findByOrganizationAndUser(organization, user)
            .orElseThrow(() -> new IdentityException("User is not a member of this organization"));

        Team team = Team.builder()
            .name(request.name())
            .organization(organization)
            .build();

        Team saved = teamRepository.save(team);

        TeamMember teamMember = TeamMember.builder()
            .team(saved)
            .user(user)
            .role(TeamRole.LEAD)
            .build();

        teamMemberRepository.save(teamMember);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getTeamsForOrganization(UUID organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new IdentityException("Organization not found"));

        return teamRepository.findByOrganization(organization).stream()
            .map(this::toResponse)
            .toList();
    }

    private TeamResponse toResponse(Team team) {
        return new TeamResponse(
            team.getId(),
            team.getName(),
            team.getOrganization().getId(),
            team.getCreatedAt(),
            team.getUpdatedAt()
        );
    }
}
