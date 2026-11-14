package com.jeep.analytics.service;

import com.jeep.analytics.dto.OrganizationAnalyticsResponse;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the cache-aside behaviour of {@link AnalyticsService} with a
 * mocked {@link CacheService}: a miss runs the COUNT queries and stores
 * the result, a hit returns the cached summary without touching the
 * counting repositories. The database-backed slice test still covers the
 * counts themselves.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceCachingTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private OrganizationMemberRepository organizationMemberRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @Mock private CacheService cacheService;

    private AnalyticsService analyticsService() {
        return new AnalyticsService(organizationRepository, organizationMemberRepository,
            teamRepository, projectRepository, taskRepository, userRepository, cacheService);
    }

    @Test
    void getOrganizationSummary_shouldCacheMissThenStore() {
        UUID orgId = UUID.randomUUID();
        Organization organization = org.mockito.Mockito.mock(Organization.class);
        when(organization.getId()).thenReturn(orgId);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(cacheService.get("analytics:org:" + orgId, OrganizationAnalyticsResponse.class))
            .thenReturn(Optional.empty());
        when(organizationMemberRepository.countByOrganization(organization)).thenReturn(2L);
        when(teamRepository.countByOrganization(organization)).thenReturn(1L);
        when(projectRepository.countByOrganization(organization)).thenReturn(3L);
        when(taskRepository.countByProject_OrganizationAndStatusNotIn(eq(organization), any()))
            .thenReturn(4L);
        when(taskRepository.countByProject_OrganizationAndStatus(organization, TaskStatus.DONE))
            .thenReturn(5L);
        when(taskRepository.countByProject_OrganizationAndStatusIn(eq(organization), any()))
            .thenReturn(1L);

        OrganizationAnalyticsResponse response = analyticsService().getOrganizationSummary(orgId);

        assertThat(response.openTasks()).isEqualTo(4);
        assertThat(response.completedTasks()).isEqualTo(5);
        verify(cacheService).put(eq("analytics:org:" + orgId), eq(response), eq(CacheService.ANALYTICS_TTL));
    }

    @Test
    void getOrganizationSummary_shouldServeFromCacheWithoutCounting() {
        UUID orgId = UUID.randomUUID();
        Organization organization = org.mockito.Mockito.mock(Organization.class);
        when(organization.getId()).thenReturn(orgId);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        OrganizationAnalyticsResponse cached =
            new OrganizationAnalyticsResponse(orgId, 2, 1, 3, 4, 5, 1);
        when(cacheService.get("analytics:org:" + orgId, OrganizationAnalyticsResponse.class))
            .thenReturn(Optional.of(cached));

        OrganizationAnalyticsResponse response = analyticsService().getOrganizationSummary(orgId);

        assertThat(response).isEqualTo(cached);
        verify(organizationMemberRepository, never()).countByOrganization(any());
        verify(teamRepository, never()).countByOrganization(any());
        verify(projectRepository, never()).countByOrganization(any());
        verify(taskRepository, never()).countByProject_OrganizationAndStatusNotIn(any(), any());
        verify(cacheService, never()).put(any(), any(), any());
    }

    @Test
    void getOrganizationSummary_withUser_shouldStillCheckMembershipBeforeCache() {
        UUID orgId = UUID.randomUUID();
        User user = org.mockito.Mockito.mock(User.class);
        Organization organization = org.mockito.Mockito.mock(Organization.class);
        when(userRepository.findByEmail("ada@jeep.example")).thenReturn(Optional.of(user));
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));
        when(organizationMemberRepository.findByOrganizationAndUser(organization, user))
            .thenReturn(Optional.empty());

        AnalyticsService service = analyticsService();
        assertThatThrownBy(() -> service.getOrganizationSummary("ada@jeep.example", orgId))
            .isInstanceOf(com.jeep.identity.exception.IdentityException.class)
            .hasMessageContaining("not a member");
        // Membership failed, so the cache must never have been consulted.
        verify(cacheService, never()).get(any(), any());
    }
}
