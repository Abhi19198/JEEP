package com.jeep.analytics.service;

import com.jeep.analytics.dto.OrganizationAnalyticsResponse;
import com.jeep.identity.exception.IdentityException;
import com.jeep.identity.domain.User;
import com.jeep.identity.repository.UserRepository;
import com.jeep.organization.domain.Organization;
import com.jeep.organization.repository.OrganizationMemberRepository;
import com.jeep.organization.repository.OrganizationRepository;
import com.jeep.project.repository.ProjectRepository;
import com.jeep.shared.cache.CacheService;
import com.jeep.task.domain.TaskStatus;
import com.jeep.task.repository.TaskRepository;
import com.jeep.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final TeamRepository teamRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CacheService cacheService;

    @Transactional(readOnly = true)
    public OrganizationAnalyticsResponse getOrganizationSummary(String userEmail, UUID organizationId) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IdentityException("User not found"));
        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new IdentityException("Organization not found"));
        organizationMemberRepository.findByOrganizationAndUser(organization, user)
            .orElseThrow(() -> new IdentityException("User is not a member of this organization"));

        return cachedSummary(organization);
    }

    @Transactional(readOnly = true)
    public OrganizationAnalyticsResponse getOrganizationSummary(UUID organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new IdentityException("Organization not found"));

        return cachedSummary(organization);
    }

    /**
     * Cache-aside on the summary only: the membership/ownership checks
     * above always run against the database first, so a cached summary
     * can never leak to a non-member. A miss runs the COUNT queries and
     * stores the result for {@link CacheService#ANALYTICS_TTL}.
     */
    private OrganizationAnalyticsResponse cachedSummary(Organization organization) {
        String key = CacheService.analyticsKey(organization.getId());
        return cacheService.get(key, OrganizationAnalyticsResponse.class)
            .orElseGet(() -> {
                OrganizationAnalyticsResponse response = summarize(organization);
                cacheService.put(key, response, CacheService.ANALYTICS_TTL);
                return response;
            });
    }

    private OrganizationAnalyticsResponse summarize(Organization organization) {
        long totalMembers = organizationMemberRepository.countByOrganization(organization);
        long totalTeams = teamRepository.countByOrganization(organization);
        long totalProjects = projectRepository.countByOrganization(organization);
        long openTasks = taskRepository.countByProject_OrganizationAndStatusNotIn(
            organization, List.of(TaskStatus.DONE, TaskStatus.CANCELLED));
        long completedTasks = taskRepository.countByProject_OrganizationAndStatus(
            organization, TaskStatus.DONE);
        long inProgressTasks = taskRepository.countByProject_OrganizationAndStatusIn(
            organization, List.of(TaskStatus.IN_PROGRESS, TaskStatus.REVIEW));

        return new OrganizationAnalyticsResponse(
            organization.getId(),
            totalMembers,
            totalTeams,
            totalProjects,
            openTasks,
            completedTasks,
            inProgressTasks
        );
    }
}
