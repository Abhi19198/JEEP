package com.jeep.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 100, message = "First name length must be between 2 and 100")
    String firstName,

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 100, message = "Last name length must be between 2 and 100")
    String lastName,

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 12, message = "Password must be at least 12 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{12,}$",
        message = "Password must contain uppercase, lowercase, number, and special character"
    )
    String password,

    @NotBlank(message = "Password confirmation is required")
    String confirmPassword
) {
    public boolean passwordsMatch() {
        return password != null && password.equals(confirmPassword);
    }
}
