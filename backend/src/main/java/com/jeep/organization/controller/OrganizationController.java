package com.jeep.organization.controller;

import com.jeep.organization.dto.CreateOrganizationRequest;
import com.jeep.organization.dto.OrganizationResponse;
import com.jeep.organization.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @PostMapping
    public ResponseEntity<OrganizationResponse> createOrganization(
        Authentication authentication,
        @Valid @RequestBody CreateOrganizationRequest request
    ) {
        String userEmail = authentication.getName();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(organizationService.createOrganization(userEmail, request));
    }

    @GetMapping
    public ResponseEntity<List<OrganizationResponse>> getOrganizations(Authentication authentication) {
        return ResponseEntity.ok(organizationService.getOrganizationsForUser(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponse> getOrganization(Authentication authentication, @PathVariable UUID id) {
        return ResponseEntity.ok(organizationService.getOrganization(authentication.getName(), id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrganizationResponse> updateOrganization(
        Authentication authentication,
        @PathVariable UUID id,
        @Valid @RequestBody CreateOrganizationRequest request
    ) {
        return ResponseEntity.ok(organizationService.updateOrganization(authentication.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateOrganization(Authentication authentication, @PathVariable UUID id) {
        organizationService.deactivateOrganization(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
