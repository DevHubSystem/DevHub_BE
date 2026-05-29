package iuh.fit.devhub_be.task.service;

import iuh.fit.devhub_be.auth.model.User;
import iuh.fit.devhub_be.common.exception.BadRequestException;
import iuh.fit.devhub_be.common.exception.ResourceNotFoundException;
import iuh.fit.devhub_be.task.dto.request.CreateTaskRequest;
import iuh.fit.devhub_be.task.dto.response.TaskResponse;
import iuh.fit.devhub_be.task.model.Priority;
import iuh.fit.devhub_be.task.model.Task;
import iuh.fit.devhub_be.task.model.TaskStatus;
import iuh.fit.devhub_be.task.model.WorkType;
import iuh.fit.devhub_be.task.repository.TaskRepository;
import iuh.fit.devhub_be.task.service.impl.TaskServiceImpl;
import iuh.fit.devhub_be.workspace.model.Workspace;
import iuh.fit.devhub_be.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private WorkspaceRepository workspaceRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

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
        workspace.setOwner(owner);
        workspace.setMembers(new HashSet<>(Set.of(members)));
        return workspace;
    }

    private Task buildEpic(UUID id, Workspace workspace) {
        Task epic = new Task();
        epic.setId(id);
        epic.setName("Epic");
        epic.setWorkType(WorkType.EPIC);
        epic.setStatus(TaskStatus.PENDING);
        epic.setPriority(Priority.MEDIUM);
        epic.setWorkspace(workspace);
        return epic;
    }

    private CreateTaskRequest minimalRequest(UUID workspaceId) {
        return new CreateTaskRequest(
                "Write docs", WorkType.TASK, workspaceId,
                null, null, null, null, null, null);
    }

    // ─── Happy paths ─────────────────────────────────────────────────────────

    @Test
    void testCreateMinimalAppliesDefaults() {
        UUID workspaceId = UUID.randomUUID();
        User owner = buildUser(UUID.randomUUID(), "duc");
        Workspace workspace = buildWorkspace(workspaceId, owner);

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse result = taskService.create(minimalRequest(workspaceId));

        assertNotNull(result);
        assertEquals("Write docs", result.name());
        assertEquals(WorkType.TASK, result.workType());
        assertEquals(TaskStatus.PENDING, result.status(), "status defaults to PENDING");
        assertEquals(Priority.MEDIUM, result.priority(), "priority defaults to MEDIUM");
        assertEquals(workspaceId, result.workspaceId());
        assertTrue(result.assignees().isEmpty());
        assertNull(result.parentTaskId());
        assertNull(result.dueDate());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void testCreateWithAllFieldsSucceeds() {
        UUID workspaceId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        User owner = buildUser(UUID.randomUUID(), "duc");
        User member = buildUser(UUID.randomUUID(), "lan");
        Workspace workspace = buildWorkspace(workspaceId, owner, member);
        Task epic = buildEpic(parentId, workspace);
        LocalDate due = LocalDate.now().plusDays(7);

        CreateTaskRequest request = new CreateTaskRequest(
                "Fix login bug", WorkType.BUG, workspaceId,
                List.of(owner.getId(), member.getId()),
                TaskStatus.IN_PROGRESS, Priority.HIGH, due,
                "Steps to reproduce...", parentId);

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(taskRepository.findById(parentId)).thenReturn(Optional.of(epic));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse result = taskService.create(request);

        assertEquals(WorkType.BUG, result.workType());
        assertEquals(TaskStatus.IN_PROGRESS, result.status());
        assertEquals(Priority.HIGH, result.priority());
        assertEquals(due, result.dueDate());
        assertEquals("Steps to reproduce...", result.description());
        assertEquals(parentId, result.parentTaskId());
        assertEquals(2, result.assignees().size());
    }

    @Test
    void testCreateWithOwnerAsAssigneeSucceeds() {
        UUID workspaceId = UUID.randomUUID();
        User owner = buildUser(UUID.randomUUID(), "duc");
        Workspace workspace = buildWorkspace(workspaceId, owner);

        CreateTaskRequest request = new CreateTaskRequest(
                "Owner task", WorkType.TASK, workspaceId,
                List.of(owner.getId()), null, null, null, null, null);

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse result = taskService.create(request);

        assertEquals(1, result.assignees().size());
        assertEquals(owner.getId(), result.assignees().get(0).id());
    }

    @Test
    void testCreateWithDueDateTodaySucceeds() {
        UUID workspaceId = UUID.randomUUID();
        Workspace workspace = buildWorkspace(workspaceId, buildUser(UUID.randomUUID(), "duc"));
        CreateTaskRequest request = new CreateTaskRequest(
                "Due today", WorkType.TASK, workspaceId,
                null, null, null, LocalDate.now(), null, null);

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse result = taskService.create(request);

        assertEquals(LocalDate.now(), result.dueDate());
    }

    // ─── Workspace errors ──────────────────────────────────────────────────

    @Test
    void testCreateWhenWorkspaceNotFound_ThrowsResourceNotFound() {
        UUID workspaceId = UUID.randomUUID();
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> taskService.create(minimalRequest(workspaceId)));

        assertEquals("Workspace not found", ex.getMessage());
        verify(taskRepository, never()).save(any());
    }

    // ─── Assignee errors ─────────────────────────────────────────────────────

    @Test
    void testCreateWhenAssigneeNotMember_ThrowsBadRequest() {
        UUID workspaceId = UUID.randomUUID();
        User owner = buildUser(UUID.randomUUID(), "duc");
        Workspace workspace = buildWorkspace(workspaceId, owner);
        UUID outsiderId = UUID.randomUUID();

        CreateTaskRequest request = new CreateTaskRequest(
                "Task", WorkType.TASK, workspaceId,
                List.of(outsiderId), null, null, null, null, null);

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> taskService.create(request));

        assertEquals("Assignee is not a member of the workspace", ex.getMessage());
        verify(taskRepository, never()).save(any());
    }

    // ─── Due date errors ─────────────────────────────────────────────────────

    @Test
    void testCreateWhenDueDateInPast_ThrowsBadRequest() {
        UUID workspaceId = UUID.randomUUID();
        Workspace workspace = buildWorkspace(workspaceId, buildUser(UUID.randomUUID(), "duc"));
        CreateTaskRequest request = new CreateTaskRequest(
                "Task", WorkType.TASK, workspaceId,
                null, null, null, LocalDate.now().minusDays(1), null, null);

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> taskService.create(request));

        assertEquals("Due date cannot be in the past", ex.getMessage());
        verify(taskRepository, never()).save(any());
    }

    // ─── Parent task errors ──────────────────────────────────────────────────

    @Test
    void testCreateWhenParentTaskNotFound_ThrowsResourceNotFound() {
        UUID workspaceId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        Workspace workspace = buildWorkspace(workspaceId, buildUser(UUID.randomUUID(), "duc"));
        CreateTaskRequest request = new CreateTaskRequest(
                "Task", WorkType.TASK, workspaceId,
                null, null, null, null, null, parentId);

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(taskRepository.findById(parentId)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> taskService.create(request));

        assertEquals("Parent task not found", ex.getMessage());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void testCreateWhenParentTaskDifferentWorkspace_ThrowsBadRequest() {
        UUID workspaceId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        Workspace workspace = buildWorkspace(workspaceId, buildUser(UUID.randomUUID(), "duc"));
        Workspace otherWorkspace = buildWorkspace(UUID.randomUUID(), buildUser(UUID.randomUUID(), "lan"));
        Task epicElsewhere = buildEpic(parentId, otherWorkspace);

        CreateTaskRequest request = new CreateTaskRequest(
                "Task", WorkType.TASK, workspaceId,
                null, null, null, null, null, parentId);

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(taskRepository.findById(parentId)).thenReturn(Optional.of(epicElsewhere));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> taskService.create(request));

        assertEquals("Parent task must belong to the same workspace", ex.getMessage());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void testCreateWhenParentTaskNotEpic_ThrowsBadRequest() {
        UUID workspaceId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        Workspace workspace = buildWorkspace(workspaceId, buildUser(UUID.randomUUID(), "duc"));
        Task parent = buildEpic(parentId, workspace);
        parent.setWorkType(WorkType.TASK); // not an EPIC

        CreateTaskRequest request = new CreateTaskRequest(
                "Sub task", WorkType.TASK, workspaceId,
                null, null, null, null, null, parentId);

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(taskRepository.findById(parentId)).thenReturn(Optional.of(parent));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> taskService.create(request));

        assertEquals("Parent task must be an EPIC", ex.getMessage());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void testCreateWithEmptyAssigneeListSucceedsUnassigned() {
        UUID workspaceId = UUID.randomUUID();
        Workspace workspace = buildWorkspace(workspaceId, buildUser(UUID.randomUUID(), "duc"));
        CreateTaskRequest request = new CreateTaskRequest(
                "Task", WorkType.TASK, workspaceId,
                List.of(), null, null, null, null, null);

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskResponse result = taskService.create(request);

        assertTrue(result.assignees().isEmpty());
    }
}
