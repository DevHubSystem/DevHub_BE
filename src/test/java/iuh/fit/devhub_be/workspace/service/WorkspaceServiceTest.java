package iuh.fit.devhub_be.workspace.service;

import iuh.fit.devhub_be.auth.model.User;
import iuh.fit.devhub_be.auth.repository.UserRepository;
import iuh.fit.devhub_be.common.exception.BadRequestException;
import iuh.fit.devhub_be.common.exception.ForbiddenException;
import iuh.fit.devhub_be.common.exception.ResourceNotFoundException;
import iuh.fit.devhub_be.common.exception.UnauthorizedException;
import iuh.fit.devhub_be.workspace.dto.request.AddMemberRequest;
import iuh.fit.devhub_be.workspace.dto.request.CreateWorkspaceRequest;
import iuh.fit.devhub_be.workspace.dto.response.WorkspaceResponse;
import iuh.fit.devhub_be.workspace.dto.response.WorkspaceSummaryResponse;
import iuh.fit.devhub_be.workspace.model.Workspace;
import iuh.fit.devhub_be.workspace.repository.WorkspaceRepository;
import iuh.fit.devhub_be.workspace.service.impl.WorkspaceServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private WorkspaceServiceImpl workspaceService;

    // ─── Fixtures ────────────────────────────────────────────────────────────

    private User buildUser(UUID id, String userName) {
        User user = new User();
        user.setId(id);
        user.setUserName(userName);
        user.setEmail(userName + "@example.com");
        return user;
    }

    private Workspace buildWorkspace(UUID id, User owner, User... members) {
        Workspace workspace = new Workspace();
        workspace.setId(id);
        workspace.setName("DevHub Team");
        workspace.setDescription("Core team workspace");
        workspace.setOwner(owner);
        workspace.setMembers(new java.util.HashSet<>(Set.of(members)));
        return workspace;
    }

    // ─── create() ────────────────────────────────────────────────────────────

    @Test
    void testCreateSuccess() {
        UUID ownerId = UUID.randomUUID();
        User owner = buildUser(ownerId, "duc");
        CreateWorkspaceRequest request = new CreateWorkspaceRequest("DevHub Team", "Core team workspace");

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkspaceResponse result = workspaceService.create(request, ownerId);

        assertNotNull(result);
        assertEquals("DevHub Team", result.name());
        assertEquals("Core team workspace", result.description());
        assertEquals(ownerId, result.owner().id());
        assertEquals("duc", result.owner().userName());
        assertTrue(result.members().isEmpty(), "new workspace starts with no members");
        verify(workspaceRepository, times(1)).save(any(Workspace.class));
    }

    @Test
    void testCreateWithNullDescription_Succeeds() {
        UUID ownerId = UUID.randomUUID();
        User owner = buildUser(ownerId, "duc");
        CreateWorkspaceRequest request = new CreateWorkspaceRequest("No Desc", null);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkspaceResponse result = workspaceService.create(request, ownerId);

        assertEquals("No Desc", result.name());
        assertNull(result.description());
    }

    @Test
    void testCreateWhenAuthenticatedUserNotFound_ThrowsUnauthorizedException() {
        UUID ownerId = UUID.randomUUID();
        CreateWorkspaceRequest request = new CreateWorkspaceRequest("DevHub Team", "desc");
        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> workspaceService.create(request, ownerId));

        verify(workspaceRepository, never()).save(any());
    }

    // ─── getById() ───────────────────────────────────────────────────────────

    @Test
    void testGetByIdAsOwner_ReturnsWorkspace() {
        UUID ownerId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        User owner = buildUser(ownerId, "duc");
        Workspace workspace = buildWorkspace(workspaceId, owner);

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        WorkspaceResponse result = workspaceService.getById(workspaceId, ownerId);

        assertEquals(workspaceId, result.id());
        assertEquals(ownerId, result.owner().id());
    }

    @Test
    void testGetByIdAsMember_ReturnsWorkspace() {
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        User owner = buildUser(ownerId, "duc");
        User member = buildUser(memberId, "lan");
        Workspace workspace = buildWorkspace(workspaceId, owner, member);

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        WorkspaceResponse result = workspaceService.getById(workspaceId, memberId);

        assertEquals(workspaceId, result.id());
        assertEquals(1, result.members().size());
        assertEquals(memberId, result.members().get(0).id());
    }

    @Test
    void testGetByIdWhenNotFound_ThrowsResourceNotFoundException() {
        UUID workspaceId = UUID.randomUUID();
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> workspaceService.getById(workspaceId, UUID.randomUUID()));

        assertEquals("Workspace not found", ex.getMessage());
    }

    @Test
    void testGetByIdWhenNotOwnerNorMember_ThrowsResourceNotFound() {
        UUID ownerId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        User owner = buildUser(ownerId, "duc");
        Workspace workspace = buildWorkspace(workspaceId, owner);

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        // Outsider gets the same 404 as a missing workspace — no existence leak.
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> workspaceService.getById(workspaceId, outsiderId));

        assertEquals("Workspace not found", ex.getMessage());
    }

    // ─── listMine() ──────────────────────────────────────────────────────────

    @Test
    void testListMineReturnsSummaries() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, "duc");
        User member = buildUser(UUID.randomUUID(), "lan");
        Workspace owned = buildWorkspace(UUID.randomUUID(), user, member);
        Workspace joined = buildWorkspace(UUID.randomUUID(), buildUser(UUID.randomUUID(), "minh"), user);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(workspaceRepository.findAllByOwnerOrMember(user)).thenReturn(List.of(owned, joined));

        List<WorkspaceSummaryResponse> result = workspaceService.listMine(userId);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).memberCount());
        verify(workspaceRepository).findAllByOwnerOrMember(user);
    }

    @Test
    void testListMineWhenNone_ReturnsEmptyList() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, "duc");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(workspaceRepository.findAllByOwnerOrMember(user)).thenReturn(List.of());

        List<WorkspaceSummaryResponse> result = workspaceService.listMine(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void testListMineWhenUserNotFound_ThrowsUnauthorizedException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> workspaceService.listMine(userId));

        verify(workspaceRepository, never()).findAllByOwnerOrMember(any());
    }

    // ─── addMember() ─────────────────────────────────────────────────────────

    @Test
    void testAddMemberAsOwner_Success() {
        UUID ownerId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        User owner = buildUser(ownerId, "duc");
        User newMember = buildUser(UUID.randomUUID(), "lan");
        Workspace workspace = buildWorkspace(workspaceId, owner);
        AddMemberRequest request = new AddMemberRequest("lan@example.com");

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(userRepository.findByEmail("lan@example.com")).thenReturn(Optional.of(newMember));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkspaceResponse result = workspaceService.addMember(workspaceId, request, ownerId);

        assertEquals(1, result.members().size());
        assertEquals("lan", result.members().get(0).userName());
        assertTrue(workspace.getMembers().contains(newMember));
        verify(workspaceRepository, times(1)).save(workspace);
    }

    @Test
    void testAddMemberWhenWorkspaceNotFound_ThrowsResourceNotFound() {
        UUID workspaceId = UUID.randomUUID();
        AddMemberRequest request = new AddMemberRequest("lan@example.com");
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> workspaceService.addMember(workspaceId, request, UUID.randomUUID()));

        assertEquals("Workspace not found", ex.getMessage());
        verify(workspaceRepository, never()).save(any());
    }

    @Test
    void testAddMemberWhenCallerIsMemberNotOwner_ThrowsForbidden() {
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        User owner = buildUser(ownerId, "duc");
        User member = buildUser(memberId, "lan");
        Workspace workspace = buildWorkspace(workspaceId, owner, member);
        AddMemberRequest request = new AddMemberRequest("minh@example.com");

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        // A member can read the workspace but cannot add members — 403, not a leak.
        assertThrows(ForbiddenException.class,
                () -> workspaceService.addMember(workspaceId, request, memberId));

        verify(userRepository, never()).findByEmail(any());
        verify(workspaceRepository, never()).save(any());
    }

    @Test
    void testAddMemberWhenCallerIsOutsider_ThrowsResourceNotFound() {
        UUID ownerId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        User owner = buildUser(ownerId, "duc");
        Workspace workspace = buildWorkspace(workspaceId, owner);
        AddMemberRequest request = new AddMemberRequest("minh@example.com");

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        // Outsider gets the same 404 as a missing workspace — no existence leak.
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> workspaceService.addMember(workspaceId, request, outsiderId));

        assertEquals("Workspace not found", ex.getMessage());
        verify(userRepository, never()).findByEmail(any());
        verify(workspaceRepository, never()).save(any());
    }

    @Test
    void testAddMemberWhenTargetUserNotFound_ThrowsResourceNotFound() {
        UUID ownerId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        User owner = buildUser(ownerId, "duc");
        Workspace workspace = buildWorkspace(workspaceId, owner);
        AddMemberRequest request = new AddMemberRequest("ghost@example.com");

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> workspaceService.addMember(workspaceId, request, ownerId));

        assertEquals("User not found", ex.getMessage());
        verify(workspaceRepository, never()).save(any());
    }

    @Test
    void testAddMemberWhenTargetIsOwner_ThrowsBadRequest() {
        UUID ownerId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        User owner = buildUser(ownerId, "duc");
        Workspace workspace = buildWorkspace(workspaceId, owner);
        AddMemberRequest request = new AddMemberRequest("duc@example.com");

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(userRepository.findByEmail("duc@example.com")).thenReturn(Optional.of(owner));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> workspaceService.addMember(workspaceId, request, ownerId));

        assertEquals("Owner is already a member of the workspace", ex.getMessage());
        verify(workspaceRepository, never()).save(any());
    }

    @Test
    void testAddMemberWhenTargetAlreadyMember_ThrowsBadRequest() {
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        User owner = buildUser(ownerId, "duc");
        User member = buildUser(memberId, "lan");
        Workspace workspace = buildWorkspace(workspaceId, owner, member);
        AddMemberRequest request = new AddMemberRequest("lan@example.com");

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(userRepository.findByEmail("lan@example.com")).thenReturn(Optional.of(member));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> workspaceService.addMember(workspaceId, request, ownerId));

        assertEquals("User is already a member of the workspace", ex.getMessage());
        verify(workspaceRepository, never()).save(any());
    }
}
