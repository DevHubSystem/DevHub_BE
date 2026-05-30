package iuh.fit.devhub_be.workspace.service;

import iuh.fit.devhub_be.workspace.dto.request.AddMemberRequest;
import iuh.fit.devhub_be.workspace.dto.request.CreateWorkspaceRequest;
import iuh.fit.devhub_be.workspace.dto.response.WorkspaceResponse;
import iuh.fit.devhub_be.workspace.dto.response.WorkspaceSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface WorkspaceService {

    WorkspaceResponse create(CreateWorkspaceRequest request, UUID ownerId);

    /**
     * Reads a workspace by its public reminder key. Only the owner or a member may
     * read it; any other caller (and an unknown key) gets a 404 (no existence leak).
     *
     * @param reminderKey   the workspace's public key (URL identifier)
     * @param currentUserId the authenticated caller
     * @return the full workspace detail
     */
    WorkspaceResponse getByReminderKey(String reminderKey, UUID currentUserId);

    List<WorkspaceSummaryResponse> listMine(UUID currentUserId);

    /**
     * Adds an existing registered user (identified by email) to a workspace.
     * Only the workspace owner may perform this action.
     *
     * @param reminderKey   the target workspace's public key (URL identifier)
     * @param request       carries the email of the user to add
     * @param currentUserId the authenticated caller (must be the owner)
     * @return the updated workspace detail
     */
    WorkspaceResponse addMember(String reminderKey, AddMemberRequest request, UUID currentUserId);
}
