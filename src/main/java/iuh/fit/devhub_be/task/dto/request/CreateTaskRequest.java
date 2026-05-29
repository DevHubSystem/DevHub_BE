package iuh.fit.devhub_be.task.dto.request;

import iuh.fit.devhub_be.task.model.Priority;
import iuh.fit.devhub_be.task.model.TaskStatus;
import iuh.fit.devhub_be.task.model.WorkType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Payload for {@code POST /tasks} (FEAT-003 §3.1).
 *
 * <p>{@code status} and {@code priority} are optional and default to
 * {@code PENDING} / {@code MEDIUM} respectively when omitted. An invalid enum
 * literal for {@code workType}/{@code status}/{@code priority} is rejected by
 * Jackson as a {@code 400} before validation runs.
 */
public record CreateTaskRequest(
        @NotBlank(message = "Task name is required")
        String name,

        @NotNull(message = "Work type is required")
        WorkType workType,

        @NotNull(message = "Workspace id is required")
        UUID workspaceId,

        List<UUID> assigneeIds,

        TaskStatus status,

        Priority priority,

        LocalDate dueDate,

        String description,

        UUID parentTaskId
) {}
