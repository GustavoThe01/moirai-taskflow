package dev.gustavo.moirai.adapter.in.web;

import dev.gustavo.moirai.application.port.in.TaskUseCases.AssignTask;
import dev.gustavo.moirai.application.port.in.TaskUseCases.CreateTask;
import dev.gustavo.moirai.application.port.in.TaskUseCases.MoveTask;
import dev.gustavo.moirai.application.port.in.TaskUseCases.NewTask;
import dev.gustavo.moirai.domain.ProjectId;
import dev.gustavo.moirai.domain.Task;
import dev.gustavo.moirai.domain.TaskId;
import dev.gustavo.moirai.domain.TaskPriority;
import dev.gustavo.moirai.domain.TaskStatus;
import dev.gustavo.moirai.domain.UserId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API de tarefas. O usuário vem do cabeçalho {@code X-User-Id} apenas até o Dia 12. */
@RestController
@RequestMapping("/api/v1")
class TaskController {

    private static final String USER_HEADER = "X-User-Id";

    private final CreateTask createTask;
    private final MoveTask moveTask;
    private final AssignTask assignTask;

    TaskController(CreateTask createTask, MoveTask moveTask, AssignTask assignTask) {
        this.createTask = createTask;
        this.moveTask = moveTask;
        this.assignTask = assignTask;
    }

    record CreateTaskRequest(
            @NotBlank @Size(min = 3, max = 120) String title,
            @Size(max = 2000) String description,
            @NotNull TaskPriority priority,
            LocalDate dueDate) {
    }

    record MoveTaskRequest(@NotNull TaskStatus status) {
    }

    record AssignTaskRequest(@NotNull UUID assigneeId) {
    }

    record TaskResponse(UUID id, UUID projectId, String title, String description, TaskPriority priority,
                        TaskStatus status, LocalDate dueDate, UUID assigneeId, UUID createdBy, Instant createdAt) {

        static TaskResponse from(Task task) {
            return new TaskResponse(
                    task.id().value(),
                    task.projectId().value(),
                    task.title(),
                    task.description(),
                    task.priority(),
                    task.status(),
                    task.dueDate().orElse(null),
                    task.assigneeId().map(UserId::value).orElse(null),
                    task.createdBy().value(),
                    task.createdAt());
        }
    }

    @PostMapping("/projects/{projectId}/tasks")
    ResponseEntity<TaskResponse> create(@RequestHeader(USER_HEADER) UUID user, @PathVariable UUID projectId,
                                        @Valid @RequestBody CreateTaskRequest request) {
        NewTask newTask = new NewTask(request.title(), request.description(), request.priority(), request.dueDate());
        Task task = createTask.create(new ProjectId(projectId), newTask, new UserId(user));
        return ResponseEntity.created(URI.create("/api/v1/tasks/" + task.id())).body(TaskResponse.from(task));
    }

    @PatchMapping("/tasks/{id}/status")
    TaskResponse move(@RequestHeader(USER_HEADER) UUID user, @PathVariable UUID id,
                      @Valid @RequestBody MoveTaskRequest request) {
        return TaskResponse.from(moveTask.move(new TaskId(id), request.status(), new UserId(user)));
    }

    @PatchMapping("/tasks/{id}/assignee")
    TaskResponse assign(@RequestHeader(USER_HEADER) UUID user, @PathVariable UUID id,
                        @Valid @RequestBody AssignTaskRequest request) {
        return TaskResponse.from(assignTask.assign(new TaskId(id), new UserId(request.assigneeId()), new UserId(user)));
    }
}
