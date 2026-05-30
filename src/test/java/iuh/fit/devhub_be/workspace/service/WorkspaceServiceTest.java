package iuh.fit.devhub_be.workspace.service;

import iuh.fit.devhub_be.auth.model.User;
import iuh.fit.devhub_be.auth.repository.UserRepository;
import iuh.fit.devhub_be.common.exception.BadRequestException;
import iuh.fit.devhub_be.common.exception.ForbiddenException;
import iuh.fit.devhub_be.common.exception.ResourceNotFoundException;
import iuh.fit.devhub_be.common.exception.UnauthorizedException;
import iuh.fit.devhub_be.notification.service.NotificationService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private WorkspaceServiceImpl workspaceService;

    private static final String KEY = "DEV001";

    // ─── Fixtures ────────────────────────────────────────────────────────────

    private User buildUser(UUID id, String userName) {
        User user = new User();
        user.setId(id);
        user.setUserName(userName);
        user.setEmail(userName + "@example.com");
        return user;
    }

    private Workspace buildWorkspace(UUID id, String reminderKey, User owner, User... members) {
        Workspace workspace = new Workspace();
        workspace.setId(id);
        workspace.setReminderKey(reminderKey);
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
        CreateWorkspaceRequest request = new CreateWorkspaceRequest("DevHub Team", "Core team workspace", KEY);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(workspaceRepository.existsByReminderKey(KEY)).thenReturn(false);
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkspaceResponse result = workspaceService.create(request, ownerId);

        assertNotNull(result);
        assertEquals(KEY, result.reminderKey());
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
        CreateWorkspaceRequest request = new CreateWorkspaceRequest("No Desc", null, KEY);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(workspaceRepository.existsByReminderKey(KEY)).thenReturn(false);
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkspaceResponse result = workspaceService.create(request, ownerId);

        assertEquals("No Desc", result.name());
        assertNull(result.description());
    }

    @Test
    void testCreateWhenReminderKeyAlreadyInUse_ThrowsBadRequest() {
        UUID ownerId = UUID.randomUUID();
        User owner = buildUser(ownerId, "duc");
        CreateWorkspaceRequest request = new CreateWorkspaceRequest("DevHub Team", "desc", KEY);

        when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
        when(workspaceRepository.existsByReminderKey(KEY)).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> workspaceService.create(request, ownerId));

        assertEquals("Reminder key is already in use", ex.getMessage());
        verify(workspaceRepository, never()).save(any());
    }

    @Test
    void testCreateWhenAuthenticatedUserNotFound_ThrowsUnauthorizedException() {
        UUID ownerId = UUID.randomUUID();
        CreateWorkspaceRequest request = new CreateWorkspaceRequest("DevHub Team", "desc", KEY);
        when(userRepository.findById(ownerId)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> workspaceService.create(request, ownerId));

        verify(workspaceRepository, never()).save(any());
    }

    // ─── getByReminderKey() ──────────────────────────────────────────────────

    @Test
    void testGetByReminderKeyAsOwner_ReturnsWorkspace() {
        UUID ownerId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        User owner = buildUser(ownerId, "duc");
        Workspace workspace = buildWorkspace(workspaceId, KEY, owner);

        when(workspaceRepository.findByReminderKey(KEY)).thenReturn(Optional.of(workspace));

        WorkspaceResponse result = workspaceService.getByReminderKey(KEY, ownerId);

        assertEquals(workspaceId, result.id());
        assertEquals(KEY, result.reminderKey());
        assertEquals(ownerId, result.owner().id());
    }

    @Test
    void testGetByReminderKeyAsMember_ReturnsWorkspace() {
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        User owner = buildUser(ownerId, "duc");
        User member = buildUser(memberId, "lan");
        Workspace workspace = buildWorkspace(workspaceId, KEY, owner, member);

        when(workspaceRepository.findByReminderKey(KEY)).thenReturn(Optional.of(workspace));

        WorkspaceResponse result = workspaceService.getByReminderKey(KEY, memberId);

        assertEquals(workspaceId, result.id());
        assertEquals(1, result.members().size());
        assertEquals(memberId, result.members().get(0).id());
    }

    @Test
    void testGetByReminderKeyWhenNotFound_ThrowsResourceNotFoundException() {
        when(workspaceRepository.findByReminderKey(KEY)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> workspaceService.getByReminderKey(KEY, UUID.randomUUID()));

        assertEquals("Workspace not found", ex.getMessage());
    }

    @Test
    void testGetByReminderKeyWhenNotOwnerNorMember_ThrowsResourceNotFound() {
        UUID ownerId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        User owner = buildUser(ownerId, "duc");
        Workspace workspace = buildWorkspace(workspaceId, KEY, owner);

        when(workspaceRepository.findByReminderKey(KEY)).thenReturn(Optional.of(workspace));

        // Outsider gets the same 404 as a missing workspace — no existence leak.
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> workspaceService.getByReminderKey(KEY, outsiderId));

        assertEquals("Workspace not found", ex.getMessage());
    }

    // ─── listMine() ──────────────────────────────────────────────────────────

    @Test
    void testListMineReturnsSummaries() {
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, "duc");
        User member = buildUser(UUID.randomUUID(), "lan");
        Workspace owned = buildWorkspace(UUID.randomUUID(), "OWN001", user, member);
        Workspace joined = buildWorkspace(UUID.randomUUID(), "JOIN01", buildUser(UUID.randomUUID(), "minh"), user);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(workspaceRepository.findAllByOwnerOrMember(user)).thenReturn(List.of(owned, joined));

        List<WorkspaceSummaryResponse> result = workspaceService.listMine(userId);

        assertEquals(2, result.size());
        assertEquals("OWN001", result.get(0).reminderKey());
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
        Workspace workspace = buildWorkspace(workspaceId, KEY, owner);
        AddMemberRequest request = new AddMemberRequest("lan@example.com");

        when(workspaceRepository.findByReminderKey(KEY)).thenReturn(Optional.of(workspace));
        when(userRepository.findByEmail("lan@example.com")).thenReturn(Optional.of(newMember));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkspaceResponse result = workspaceService.addMember(KEY, request, ownerId);

        assertEquals(1, result.members().size());
        assertEquals("lan", result.members().get(0).userName());
        assertTrue(workspace.getMembers().contains(newMember));
        verify(workspaceRepository, times(1)).save(workspace);
        // The newly added member is notified with the workspace's id + name.
        verify(notificationService, times(1))
                .notifyWorkspaceMemberAdded(eq(newMember), eq(workspaceId), eq("DevHub Team"));
    }

    @Test
    void testAddMemberWhenWorkspaceNotFound_ThrowsResourceNotFound() {
        AddMemberRequest request = new AddMemberRequest("lan@example.com");
        when(workspaceRepository.findByReminderKey(KEY)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> workspaceService.addMember(KEY, request, UUID.randomUUID()));

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
        Workspace workspace = buildWorkspace(workspaceId, KEY, owner, member);
        AddMemberRequest request = new AddMemberRequest("minh@example.com");

        when(workspaceRepository.findByReminderKey(KEY)).thenReturn(Optional.of(workspace));

        // A member can read the workspace but cannot add members — 403, not a leak.
        assertThrows(ForbiddenException.class,
                () -> workspaceService.addMember(KEY, request, memberId));

        verify(userRepository, never()).findByEmail(any());
        verify(workspaceRepository, never()).save(any());
    }

    @Test
    void testAddMemberWhenCallerIsOutsider_ThrowsResourceNotFound() {
        UUID ownerId = UUID.randomUUID();
        UUID outsiderId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        User owner = buildUser(ownerId, "duc");
        Workspace workspace = buildWorkspace(workspaceId, KEY, owner);
        AddMemberRequest request = new AddMemberRequest("minh@example.com");

        when(workspaceRepository.findByReminderKey(KEY)).thenReturn(Optional.of(workspace));

        // Outsider gets the same 404 as a missing workspace — no existence leak.
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> workspaceService.addMember(KEY, request, outsiderId));

        assertEquals("Workspace not found", ex.getMessage());
        verify(userRepository, never()).findByEmail(any());
        verify(workspaceRepository, never()).save(any());
    }

    @Test
    void testAddMemberWhenTargetUserNotFound_ThrowsResourceNotFound() {
        UUID ownerId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        User owner = buildUser(ownerId, "duc");
        Workspace workspace = buildWorkspace(workspaceId, KEY, owner);
        AddMemberRequest request = new AddMemberRequest("ghost@example.com");

        when(workspaceRepository.findByReminderKey(KEY)).thenReturn(Optional.of(workspace));
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> workspaceService.addMember(KEY, request, ownerId));

        assertEquals("User not found", ex.getMessage());
        verify(workspaceRepository, never()).save(any());
    }

    @Test
    void testAddMemberWhenTargetIsOwner_ThrowsBadRequest() {
        UUID ownerId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        User owner = buildUser(ownerId, "duc");
        Workspace workspace = buildWorkspace(workspaceId, KEY, owner);
        AddMemberRequest request = new AddMemberRequest("duc@example.com");

        when(workspaceRepository.findByReminderKey(KEY)).thenReturn(Optional.of(workspace));
        when(userRepository.findByEmail("duc@example.com")).thenReturn(Optional.of(owner));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> workspaceService.addMember(KEY, request, ownerId));

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
        Workspace workspace = buildWorkspace(workspaceId, KEY, owner, member);
        AddMemberRequest request = new AddMemberRequest("lan@example.com");

        when(workspaceRepository.findByReminderKey(KEY)).thenReturn(Optional.of(workspace));
        when(userRepository.findByEmail("lan@example.com")).thenReturn(Optional.of(member));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> workspaceService.addMember(KEY, request, ownerId));

        assertEquals("User is already a member of the workspace", ex.getMessage());
        verify(workspaceRepository, never()).save(any());
    }
}
