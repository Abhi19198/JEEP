package com.jeep.analytics.controller;

import com.jeep.analytics.dto.OrganizationAnalyticsResponse;
import com.jeep.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/organizations/{organizationId}")
    public ResponseEntity<OrganizationAnalyticsResponse> getOrganizationSummary(Authentication authentication, @PathVariable UUID organizationId) {
        return ResponseEntity.ok(analyticsService.getOrganizationSummary(authentication.getName(), organizationId));
    }
}
