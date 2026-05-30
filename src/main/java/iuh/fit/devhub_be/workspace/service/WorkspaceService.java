package iuh.fit.devhub_be.workspace.service;

import iuh.fit.devhub_be.workspace.dto.request.AddMemberRequest;
import iuh.fit.devhub_be.workspace.dto.request.CreateWorkspaceRequest;
import iuh.fit.devhub_be.workspace.dto.response.WorkspaceResponse;
import iuh.fit.devhub_be.workspace.dto.response.WorkspaceSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface WorkspaceService {

    WorkspaceResponse create(CreateWorkspaceRequest request, UUID ownerId);

    WorkspaceResponse getById(UUID workspaceId, UUID currentUserId);

    List<WorkspaceSummaryResponse> listMine(UUID currentUserId);

    /**
     * Adds an existing registered user (identified by email) to a workspace.
     * Only the workspace owner may perform this action.
     *
     * @param workspaceId   the target workspace
     * @param request       carries the email of the user to add
     * @param currentUserId the authenticated caller (must be the owner)
     * @return the updated workspace detail
     */
    WorkspaceResponse addMember(UUID workspaceId, AddMemberRequest request, UUID currentUserId);
}
