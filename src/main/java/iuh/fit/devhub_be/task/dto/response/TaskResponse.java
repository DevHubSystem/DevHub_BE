package iuh.fit.devhub_be.task.dto.response;

import iuh.fit.devhub_be.task.model.Priority;
import iuh.fit.devhub_be.task.model.TaskStatus;
import iuh.fit.devhub_be.task.model.WorkType;
import iuh.fit.devhub_be.workspace.dto.response.UserSummary;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Full task detail returned by {@code POST /tasks}.
 *
 * <p>Assignees are exposed as the workspace module's slim {@link UserSummary}
 * (id + userName only) so sensitive user fields are never leaked.
 */
public record TaskResponse(
        UUID id,
        String name,
        WorkType workType,
        TaskStatus status,
        Priority priority,
        LocalDate dueDate,
        String description,
        UUID workspaceId,
        List<UserSummary> assignees,
        UUID parentTaskId,
        Instant createdAt,
        Instant updatedAt
) {}
