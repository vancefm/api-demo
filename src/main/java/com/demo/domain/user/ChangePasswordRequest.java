package com.demo.domain.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for self-service password change.
 */
@Schema(description = "Self-service password change request")
public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        @Schema(description = "The user's current password")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Schema(description = "The new password")
        String newPassword) {
}
