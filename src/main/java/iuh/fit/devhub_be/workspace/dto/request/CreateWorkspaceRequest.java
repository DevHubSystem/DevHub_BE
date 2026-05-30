package iuh.fit.devhub_be.workspace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateWorkspaceRequest(
        @NotBlank(message = "Workspace name is required")
        String name,

        String description,

        /**
         * Unique workspace key used as the identifier in URLs. Exactly 6 characters,
         * uppercase letters (A-Z) and digits (0-9) only, and must contain at least one
         * letter (e.g. "DEV001" or "ABCDEF" are valid; "123456" is not).
         */
        @NotBlank(message = "Reminder key is required")
        @Pattern(
                regexp = "^(?=.*[A-Z])[A-Z0-9]{6}$",
                message = "Reminder key must be exactly 6 characters of uppercase letters and digits, including at least one letter"
        )
        String reminderKey
) {}