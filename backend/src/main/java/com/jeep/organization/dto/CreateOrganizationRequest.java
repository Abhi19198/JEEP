package com.jeep.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
    @NotBlank(message = "Organization name is required")
    @Size(min = 2, max = 150, message = "Organization name must be between 2 and 150 chars")
    String name,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description
) {
}
