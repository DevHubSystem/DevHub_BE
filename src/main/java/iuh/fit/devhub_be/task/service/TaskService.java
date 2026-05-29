package iuh.fit.devhub_be.task.service;

import iuh.fit.devhub_be.task.dto.request.CreateTaskRequest;
import iuh.fit.devhub_be.task.dto.response.TaskResponse;

public interface TaskService {

    TaskResponse create(CreateTaskRequest request);
}
