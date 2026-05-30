package iuh.fit.devhub_be.workspace.controller;

import iuh.fit.devhub_be.common.dto.ApiResponse;
import iuh.fit.devhub_be.workspace.dto.request.AddMemberRequest;
import iuh.fit.devhub_be.workspace.dto.request.CreateWorkspaceRequest;
import iuh.fit.devhub_be.workspace.dto.response.WorkspaceResponse;
import iuh.fit.devhub_be.workspace.dto.response.WorkspaceSummaryResponse;
import iuh.fit.devhub_be.workspace.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceResponse>> create(
            @RequestBody @Valid CreateWorkspaceRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        WorkspaceResponse created = workspaceService.create(request, currentUserId(jwt));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Workspace created successfully"));
    }

    @GetMapping("/{reminderKey}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getByReminderKey(
            @PathVariable String reminderKey,
            @AuthenticationPrincipal Jwt jwt) {
        WorkspaceResponse workspace = workspaceService.getByReminderKey(reminderKey, currentUserId(jwt));
        return ResponseEntity.ok(ApiResponse.success(workspace));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceSummaryResponse>>> listMine(
            @AuthenticationPrincipal Jwt jwt) {
        List<WorkspaceSummaryResponse> workspaces = workspaceService.listMine(currentUserId(jwt));
        return ResponseEntity.ok(ApiResponse.success(workspaces));
    }

    @PostMapping("/{reminderKey}/members")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> addMember(
            @PathVariable String reminderKey,
            @RequestBody @Valid AddMemberRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        WorkspaceResponse updated = workspaceService.addMember(reminderKey, request, currentUserId(jwt));
        return ResponseEntity.ok(ApiResponse.success(updated, "Member added successfully"));
    }

    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
