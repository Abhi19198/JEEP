package com.jeep.team.controller;

import com.jeep.team.dto.CreateTeamRequest;
import com.jeep.team.dto.TeamMemberResponse;
import com.jeep.team.dto.TeamResponse;
import com.jeep.team.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(
        Authentication authentication,
        @Valid @RequestBody CreateTeamRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(teamService.createTeam(authentication.getName(), request));
    }

    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<List<TeamResponse>> getTeamsForOrganization(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(teamService.getTeamsForOrganization(organizationId));
    }

    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<TeamMemberResponse>> getTeamMembers(@PathVariable UUID teamId) {
        return ResponseEntity.ok(teamService.getTeamMembers(teamId));
    }
}
