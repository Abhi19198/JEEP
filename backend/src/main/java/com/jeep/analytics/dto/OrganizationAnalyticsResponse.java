package com.jeep.analytics.dto;

import java.util.UUID;

public record OrganizationAnalyticsResponse(
    UUID organizationId,
    long totalMembers,
    long totalTeams,
    long totalProjects,
    long openTasks,
    long completedTasks,
    long inProgressTasks
) {
}
