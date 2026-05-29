package iuh.fit.devhub_be.workspace.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full workspace detail returned by create and get-by-id.
 */
public record WorkspaceResponse(
        UUID id,
        String name,
        String description,
        UserSummary owner,
        List<UserSummary> members,
        Instant createdAt,
        Instant updatedAt
) {}
