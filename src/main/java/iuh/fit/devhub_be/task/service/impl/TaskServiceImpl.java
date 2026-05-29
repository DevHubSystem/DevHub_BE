package iuh.fit.devhub_be.task.service.impl;

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
import iuh.fit.devhub_be.task.service.TaskService;
import iuh.fit.devhub_be.workspace.dto.response.UserSummary;
import iuh.fit.devhub_be.workspace.model.Workspace;
import iuh.fit.devhub_be.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final WorkspaceRepository workspaceRepository;

    @Override
    @Transactional
    public TaskResponse create(CreateTaskRequest request) {
        Workspace workspace = workspaceRepository.findById(request.workspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));

        validateDueDate(request.dueDate());

        Set<User> assignees = resolveAssignees(request.assigneeIds(), workspace);
        Task parentTask = resolveParentTask(request.parentTaskId(), workspace);

        Task task = new Task();
        task.setName(request.name());
        task.setWorkType(request.workType());
        task.setStatus(request.status() != null ? request.status() : TaskStatus.PENDING);
        task.setPriority(request.priority() != null ? request.priority() : Priority.MEDIUM);
        task.setDueDate(request.dueDate());
        task.setDescription(request.description());
        task.setWorkspace(workspace);
        task.setAssignees(assignees);
        task.setParentTask(parentTask);

        Task saved = taskRepository.save(task);
        return toResponse(saved);
    }

    private void validateDueDate(LocalDate dueDate) {
        if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Due date cannot be in the past");
        }
    }

    /**
     * Resolves the requested assignee ids to {@link User}s drawn from the
     * workspace's member pool (owner counts as a member). Any id that is not a
     * member — including ids of non-existent users — is rejected with a 400.
     */
    private Set<User> resolveAssignees(List<UUID> assigneeIds, Workspace workspace) {
        if (assigneeIds == null || assigneeIds.isEmpty()) {
            return new HashSet<>();
        }

        Map<UUID, User> memberPool = new LinkedHashMap<>();
        memberPool.put(workspace.getOwner().getId(), workspace.getOwner());
        for (User member : workspace.getMembers()) {
            memberPool.put(member.getId(), member);
        }

        Set<User> assignees = new HashSet<>();
        for (UUID assigneeId : assigneeIds) {
            User user = memberPool.get(assigneeId);
            if (user == null) {
                throw new BadRequestException("Assignee is not a member of the workspace");
            }
            assignees.add(user);
        }
        return assignees;
    }

    private Task resolveParentTask(UUID parentTaskId, Workspace workspace) {
        if (parentTaskId == null) {
            return null;
        }

        Task parent = taskRepository.findById(parentTaskId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent task not found"));

        if (!parent.getWorkspace().getId().equals(workspace.getId())) {
            throw new BadRequestException("Parent task must belong to the same workspace");
        }
        if (parent.getWorkType() != WorkType.EPIC) {
            throw new BadRequestException("Parent task must be an EPIC");
        }
        return parent;
    }

    private TaskResponse toResponse(Task task) {
        List<UserSummary> assignees = task.getAssignees().stream()
                .map(user -> new UserSummary(user.getId(), user.getUserName()))
                .toList();
        return new TaskResponse(
                task.getId(),
                task.getName(),
                task.getWorkType(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                task.getDescription(),
                task.getWorkspace().getId(),
                assignees,
                task.getParentTask() != null ? task.getParentTask().getId() : null,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
