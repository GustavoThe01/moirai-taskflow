package dev.gustavo.moirai.adapter.in.web;

import dev.gustavo.moirai.application.port.in.TaskUseCases.AssignTask;
import dev.gustavo.moirai.application.port.in.TaskUseCases.CreateTask;
import dev.gustavo.moirai.application.port.in.TaskUseCases.ListTasks;
import dev.gustavo.moirai.application.port.in.TaskUseCases.MoveTask;
import dev.gustavo.moirai.application.port.in.TaskUseCases.NewTask;
import dev.gustavo.moirai.application.query.TaskQuery.Filter;
import dev.gustavo.moirai.application.query.TaskQuery.Page;
import dev.gustavo.moirai.application.query.TaskQuery.PageRequest;
import dev.gustavo.moirai.application.query.TaskQuery.SortField;
import dev.gustavo.moirai.domain.InvalidValueException;
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
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API de tarefas. O usuário vem do cabeçalho {@code X-User-Id} apenas até o Dia 12. */
@RestController
@RequestMapping("/api/v1")
class TaskController {

    private static final String USER_HEADER = "X-User-Id";

    private final CreateTask createTask;
    private final MoveTask moveTask;
    private final AssignTask assignTask;
    private final ListTasks listTasks;

    TaskController(CreateTask createTask, MoveTask moveTask, AssignTask assignTask, ListTasks listTasks) {
        this.createTask = createTask;
        this.moveTask = moveTask;
        this.assignTask = assignTask;
        this.listTasks = listTasks;
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

    record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages, boolean hasNext) {

        static <T> PageResponse<T> from(Page<T> page) {
            return new PageResponse<>(page.items(), page.page(), page.size(), page.totalItems(), page.totalPages(),
                    page.hasNext());
        }
    }

    /**
     * Lista as tarefas do projeto. Todos os filtros são opcionais.
     * A ordenação segue o formato {@code campo} ou {@code campo,desc}, com campo em createdAt, dueDate ou title.
     */
    @GetMapping("/projects/{projectId}/tasks")
    PageResponse<TaskResponse> list(@RequestHeader(USER_HEADER) UUID user, @PathVariable UUID projectId,
                                    @RequestParam(required = false) TaskStatus status,
                                    @RequestParam(required = false) TaskPriority priority,
                                    @RequestParam(required = false) UUID assigneeId,
                                    @RequestParam(required = false) LocalDate dueFrom,
                                    @RequestParam(required = false) LocalDate dueTo,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(defaultValue = "createdAt") String sort) {
        Filter filter = new Filter(status, priority, assigneeId == null ? null : new UserId(assigneeId), dueFrom, dueTo);
        PageRequest request = pageRequest(page, size, sort);
        return PageResponse.from(listTasks.list(new ProjectId(projectId), filter, request, new UserId(user))
                .map(TaskResponse::from));
    }

    private static PageRequest pageRequest(int page, int size, String sort) {
        String[] parts = sort.split(",", -1);
        if (parts.length > 2) {
            throw new InvalidValueException("Use a ordenação no formato campo ou campo,desc.");
        }
        SortField field = switch (parts[0].trim()) {
            case "createdAt" -> SortField.CREATED_AT;
            case "dueDate" -> SortField.DUE_DATE;
            case "title" -> SortField.TITLE;
            default -> throw new InvalidValueException(
                    "Ordenação desconhecida: " + parts[0].trim() + ". Use createdAt, dueDate ou title.");
        };
        String direction = parts.length == 1 ? "asc" : parts[1].trim().toLowerCase(Locale.ROOT);
        boolean ascending = switch (direction) {
            case "asc" -> true;
            case "desc" -> false;
            default -> throw new InvalidValueException("A direção da ordenação deve ser asc ou desc.");
        };
        return new PageRequest(page, size, field, ascending);
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
