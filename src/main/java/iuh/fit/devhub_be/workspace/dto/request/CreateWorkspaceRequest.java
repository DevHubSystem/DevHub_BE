package iuh.fit.devhub_be.workspace.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkspaceRequest(
        @NotBlank(message = "Workspace name is required")
        String name,

        String description
) {}
