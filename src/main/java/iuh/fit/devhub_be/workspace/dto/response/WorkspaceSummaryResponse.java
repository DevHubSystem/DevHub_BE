package iuh.fit.devhub_be.workspace.dto.response;

import java.util.UUID;

/**
 * Slim workspace view used by the list endpoint — no members array, just a count.
 */
public record WorkspaceSummaryResponse(
        UUID id,
        String name,
        String description,
        UserSummary owner,
        int memberCount
) {}
