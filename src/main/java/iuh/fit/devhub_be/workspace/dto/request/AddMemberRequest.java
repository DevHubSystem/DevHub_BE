package iuh.fit.devhub_be.workspace.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for adding an existing registered user to a workspace.
 * The user is identified by their email.
 */
public record AddMemberRequest(
        @NotBlank(message = "Member email is required")
        @Email(message = "Member email must be a valid email address")
        String email
) {}