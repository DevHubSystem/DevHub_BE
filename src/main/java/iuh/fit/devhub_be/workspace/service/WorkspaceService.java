package iuh.fit.devhub_be.workspace.service;

import iuh.fit.devhub_be.workspace.dto.request.CreateWorkspaceRequest;
import iuh.fit.devhub_be.workspace.dto.response.WorkspaceResponse;
import iuh.fit.devhub_be.workspace.dto.response.WorkspaceSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface WorkspaceService {

    WorkspaceResponse create(CreateWorkspaceRequest request, UUID ownerId);

    WorkspaceResponse getById(UUID workspaceId, UUID currentUserId);

    List<WorkspaceSummaryResponse> listMine(UUID currentUserId);
}
