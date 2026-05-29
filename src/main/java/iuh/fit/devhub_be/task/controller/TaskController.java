package iuh.fit.devhub_be.task.controller;

import iuh.fit.devhub_be.common.dto.ApiResponse;
import iuh.fit.devhub_be.task.dto.request.CreateTaskRequest;
import iuh.fit.devhub_be.task.dto.response.TaskResponse;
import iuh.fit.devhub_be.task.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> create(
            @RequestBody @Valid CreateTaskRequest request) {
        TaskResponse created = taskService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Task created successfully"));
    }
}
