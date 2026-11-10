package dev.gustavo.moirai.adapter.in.web;

import dev.gustavo.moirai.application.port.in.TaskUseCases.AssignTask;
import dev.gustavo.moirai.application.port.in.TaskUseCases.CreateTask;
import dev.gustavo.moirai.application.port.in.TaskUseCases.ListTasks;
import dev.gustavo.moirai.application.port.in.TaskUseCases.MoveTask;
import dev.gustavo.moirai.application.port.in.TaskUseCases.NewTask;
import dev.gustavo.moirai.application.port.in.TaskUseCases.RelabelTask;
import dev.gustavo.moirai.application.query.TaskQuery.Filter;
import dev.gustavo.moirai.application.query.TaskQuery.Page;
import dev.gustavo.moirai.application.query.TaskQuery.PageRequest;
import dev.gustavo.moirai.application.query.TaskQuery.SortField;
import dev.gustavo.moirai.domain.InvalidValueException;
import dev.gustavo.moirai.domain.LabelId;
import dev.gustavo.moirai.domain.ProjectId;
import dev.gustavo.moirai.domain.Task;
import dev.gustavo.moirai.domain.TaskId;
import dev.gustavo.moirai.domain.TaskPriority;
import dev.gustavo.moirai.domain.TaskStatus;
import dev.gustavo.moirai.domain.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API de tarefas. O usuário vem do cabeçalho {@code X-User-Id} apenas até o Dia 12. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Tarefas")
class TaskController {

    private static final String USER_HEADER = "X-User-Id";

    private final CreateTask createTask;
    private final MoveTask moveTask;
    private final AssignTask assignTask;
    private final ListTasks listTasks;
    private final RelabelTask relabelTask;

    TaskController(CreateTask createTask, MoveTask moveTask, AssignTask assignTask, ListTasks listTasks,
                   RelabelTask relabelTask) {
        this.createTask = createTask;
        this.moveTask = moveTask;
        this.assignTask = assignTask;
        this.listTasks = listTasks;
        this.relabelTask = relabelTask;
    }

    record CreateTaskRequest(
            @Schema(example = "Escrever os testes da API") @NotBlank @Size(min = 3, max = 120) String title,
            @Schema(example = "Cobrir os casos de erro com ProblemDetail") @Size(max = 2000) String description,
            @Schema(example = "HIGH") @NotNull TaskPriority priority,
            @Schema(description = "Prazo; não pode estar no passado", example = "2099-12-31") LocalDate dueDate) {
    }

    record MoveTaskRequest(
            @Schema(description = "Status de destino no fluxo TODO → IN_PROGRESS → REVIEW → DONE", example = "IN_PROGRESS")
            @NotNull TaskStatus status) {
    }

    record RelabelTaskRequest(
            @Schema(description = "Etiquetas do projeto; lista vazia remove todas") @NotNull @Size(max = 10) Set<UUID> labelIds) {
    }

    record AssignTaskRequest(@Schema(description = "Membro do projeto que vai assumir a tarefa") @NotNull UUID assigneeId) {
    }

    record TaskResponse(UUID id, UUID projectId, String title, String description, TaskPriority priority,
                        TaskStatus status, LocalDate dueDate, UUID assigneeId, UUID createdBy, Instant createdAt,
                        Set<UUID> labelIds) {

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
                    task.createdAt(),
                    task.labels().stream().map(LabelId::value).collect(Collectors.toSet()));
        }
    }

    @Schema(description = "Uma página de resultados; a primeira página é a 0")
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
    @Operation(summary = "Lista as tarefas do projeto",
            description = "Filtros opcionais e combináveis. Só membros do projeto podem listar.")
    @ApiResponse(responseCode = "200", description = "Página de tarefas")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "403", ref = "Forbidden")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    PageResponse<TaskResponse> list(@RequestHeader(USER_HEADER) UUID user, @PathVariable UUID projectId,
                                    @Parameter(description = "Só as tarefas neste status")
                                    @RequestParam(required = false) TaskStatus status,
                                    @Parameter(description = "Só as tarefas com esta prioridade")
                                    @RequestParam(required = false) TaskPriority priority,
                                    @Parameter(description = "Só as tarefas deste responsável")
                                    @RequestParam(required = false) UUID assigneeId,
                                    @Parameter(description = "Prazo a partir desta data (inclusive)", example = "2026-11-01")
                                    @RequestParam(required = false) LocalDate dueFrom,
                                    @Parameter(description = "Prazo até esta data (inclusive)", example = "2026-12-31")
                                    @RequestParam(required = false) LocalDate dueTo,
                                    @Parameter(description = "Página, começando em 0")
                                    @RequestParam(defaultValue = "0") int page,
                                    @Parameter(description = "Itens por página, de 1 a 100")
                                    @RequestParam(defaultValue = "20") int size,
                                    @Parameter(description = "createdAt, dueDate ou title; acrescente ,desc para inverter",
                                            example = "dueDate,desc")
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
    @Operation(summary = "Cria uma tarefa", description = "A tarefa nasce em TODO e sem responsável.")
    @ApiResponse(responseCode = "201", description = "Tarefa criada; o cabeçalho Location aponta para ela")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "403", ref = "Forbidden")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    ResponseEntity<TaskResponse> create(@RequestHeader(USER_HEADER) UUID user, @PathVariable UUID projectId,
                                        @Valid @RequestBody CreateTaskRequest request) {
        NewTask newTask = new NewTask(request.title(), request.description(), request.priority(), request.dueDate());
        Task task = createTask.create(new ProjectId(projectId), newTask, new UserId(user));
        return ResponseEntity.created(URI.create("/api/v1/tasks/" + task.id())).body(TaskResponse.from(task));
    }

    @PatchMapping("/tasks/{id}/status")
    @Operation(summary = "Move a tarefa no fluxo",
            description = "Avança uma etapa por vez e pode voltar uma etapa antes de DONE. Iniciar exige responsável.")
    @ApiResponse(responseCode = "200", description = "Tarefa no status novo")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "403", ref = "Forbidden")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "422", ref = "UnprocessableContent")
    TaskResponse move(@RequestHeader(USER_HEADER) UUID user, @PathVariable UUID id,
                      @Valid @RequestBody MoveTaskRequest request) {
        return TaskResponse.from(moveTask.move(new TaskId(id), request.status(), new UserId(user)));
    }

    @PutMapping("/tasks/{id}/labels")
    @Operation(summary = "Troca as etiquetas da tarefa",
            description = "Substitui o conjunto inteiro. As etiquetas precisam ser do mesmo projeto, no máximo 10.")
    @ApiResponse(responseCode = "200", description = "Tarefa com as etiquetas novas")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "403", ref = "Forbidden")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "422", ref = "UnprocessableContent")
    TaskResponse relabel(@RequestHeader(USER_HEADER) UUID user, @PathVariable UUID id,
                         @Valid @RequestBody RelabelTaskRequest request) {
        Set<LabelId> labels = request.labelIds().stream().map(LabelId::new).collect(Collectors.toSet());
        return TaskResponse.from(relabelTask.relabel(new TaskId(id), labels, new UserId(user)));
    }

    @PatchMapping("/tasks/{id}/assignee")
    @Operation(summary = "Atribui um responsável", description = "O responsável precisa ser membro do projeto.")
    @ApiResponse(responseCode = "200", description = "Tarefa com o responsável novo")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "403", ref = "Forbidden")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    TaskResponse assign(@RequestHeader(USER_HEADER) UUID user, @PathVariable UUID id,
                        @Valid @RequestBody AssignTaskRequest request) {
        return TaskResponse.from(assignTask.assign(new TaskId(id), new UserId(request.assigneeId()), new UserId(user)));
    }
}
