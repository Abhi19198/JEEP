package com.jeep.team.dto;

import com.jeep.team.domain.TeamRole;

import java.util.UUID;

public record TeamMemberResponse(
    UUID userId,
    String firstName,
    String lastName,
    String email,
    TeamRole role
) {
}
