package iuh.fit.devhub_be.workspace.dto.response;

import java.util.UUID;

/**
 * Lightweight view of a user (owner / member) — deliberately excludes email
 * and other sensitive fields exposed by the auth module's UserResponse.
 */
public record UserSummary(
        UUID id,
        String userName
) {}
