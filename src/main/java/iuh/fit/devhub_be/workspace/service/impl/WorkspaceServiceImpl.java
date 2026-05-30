package iuh.fit.devhub_be.workspace.service.impl;

import iuh.fit.devhub_be.auth.model.User;
import iuh.fit.devhub_be.auth.repository.UserRepository;
import iuh.fit.devhub_be.common.exception.BadRequestException;
import iuh.fit.devhub_be.common.exception.ForbiddenException;
import iuh.fit.devhub_be.common.exception.ResourceNotFoundException;
import iuh.fit.devhub_be.common.exception.UnauthorizedException;
import iuh.fit.devhub_be.workspace.dto.request.AddMemberRequest;
import iuh.fit.devhub_be.workspace.dto.request.CreateWorkspaceRequest;
import iuh.fit.devhub_be.workspace.dto.response.UserSummary;
import iuh.fit.devhub_be.workspace.dto.response.WorkspaceResponse;
import iuh.fit.devhub_be.workspace.dto.response.WorkspaceSummaryResponse;
import iuh.fit.devhub_be.workspace.model.Workspace;
import iuh.fit.devhub_be.workspace.repository.WorkspaceRepository;
import iuh.fit.devhub_be.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public WorkspaceResponse create(CreateWorkspaceRequest request, UUID ownerId) {
        User owner = getUser(ownerId);

        Workspace workspace = new Workspace();
        workspace.setName(request.name());
        workspace.setDescription(request.description());
        workspace.setOwner(owner);

        Workspace saved = workspaceRepository.save(workspace);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceResponse getById(UUID workspaceId, UUID currentUserId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        // Unauthorized callers get the same 404 as a missing workspace (no existence leak).
        if (!hasAccess(workspace, currentUserId)) {
            throw new ResourceNotFoundException("Workspace not found");
        }

        return toResponse(workspace);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceSummaryResponse> listMine(UUID currentUserId) {
        User user = getUser(currentUserId);
        return workspaceRepository.findAllByOwnerOrMember(user).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public WorkspaceResponse addMember(UUID workspaceId, AddMemberRequest request, UUID currentUserId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        // Owner-only write. A member (who can already read the workspace) gets 403;
        // an outsider gets the same 404 as a missing workspace — no existence leak.
        if (!workspace.getOwner().getId().equals(currentUserId)) {
            if (isMember(workspace, currentUserId)) {
                throw new ForbiddenException("Only the workspace owner can add members");
            }
            throw new ResourceNotFoundException("Workspace not found");
        }

        User target = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (workspace.getOwner().getId().equals(target.getId())) {
            throw new BadRequestException("Owner is already a member of the workspace");
        }
        if (isMember(workspace, target.getId())) {
            throw new BadRequestException("User is already a member of the workspace");
        }

        workspace.getMembers().add(target);
        Workspace saved = workspaceRepository.save(workspace);
        return toResponse(saved);
    }

    private boolean isMember(Workspace workspace, UUID userId) {
        return workspace.getMembers().stream()
                .anyMatch(member -> member.getId().equals(userId));
    }

    private boolean hasAccess(Workspace workspace, UUID userId) {
        return workspace.getOwner().getId().equals(userId) || isMember(workspace, userId);
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found"));
    }

    private WorkspaceResponse toResponse(Workspace workspace) {
        List<UserSummary> members = workspace.getMembers().stream()
                .map(this::toUserSummary)
                .toList();
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getDescription(),
                toUserSummary(workspace.getOwner()),
                members,
                workspace.getCreatedAt(),
                workspace.getUpdatedAt()
        );
    }

    private WorkspaceSummaryResponse toSummary(Workspace workspace) {
        return new WorkspaceSummaryResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getDescription(),
                toUserSummary(workspace.getOwner()),
                workspace.getMembers().size()
        );
    }

    private UserSummary toUserSummary(User user) {
        return new UserSummary(user.getId(), user.getUserName());
    }
}
