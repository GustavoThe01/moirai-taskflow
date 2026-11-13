package dev.gustavo.moirai.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import dev.gustavo.moirai.domain.InvalidTaskTransitionException;
import dev.gustavo.moirai.domain.NotProjectMemberException;
import dev.gustavo.moirai.domain.Project;
import dev.gustavo.moirai.domain.ProjectId;
import dev.gustavo.moirai.domain.Task;
import dev.gustavo.moirai.domain.TaskId;
import dev.gustavo.moirai.domain.TaskPriority;
import dev.gustavo.moirai.domain.TaskStatus;
import dev.gustavo.moirai.domain.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Fatia web: só o controller, o tratamento de erros e o Jackson sobem. Os casos de uso são mocks,
 * então o teste confere o contrato HTTP (rota, validação, conversão, status) sem banco e sem domínio rodando.
 */
@WebMvcTest(TaskController.class)
@Import({dev.gustavo.moirai.config.SecurityTestImports.class})
@DisplayName("TaskController (@WebMvcTest)")
class TaskControllerWebMvcTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-09T12:00:00Z"), ZoneOffset.UTC);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CreateTask createTask;

    @MockitoBean
    private MoveTask moveTask;

    @MockitoBean
    private AssignTask assignTask;

    @MockitoBean
    private ListTasks listTasks;

    @MockitoBean
    private RelabelTask relabelTask;

    private final UserId owner = UserId.newId();
    private final Project project = Project.create("Moirai", owner);

    private Task task() {
        return Task.create(project, owner, "Escrever testes", "Fatia web", TaskPriority.HIGH, LocalDate.of(2026, 12, 1), CLOCK);
    }

    @Test
    @DisplayName("POST converte o corpo em NewTask, devolve 201 e o Location")
    void create() throws Exception {
        Task task = task();
        when(createTask.create(eq(project.id()), any(), eq(owner))).thenReturn(task);

        mvc.perform(post("/api/v1/projects/{id}/tasks", project.id().value())
                        .with(TestUsers.as(owner.value()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Escrever testes\", \"description\": \"Fatia web\", \"priority\": \"HIGH\", \"dueDate\": \"2026-12-01\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/tasks/" + task.id())))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.dueDate").value("2026-12-01"));

        ArgumentCaptor<NewTask> captor = ArgumentCaptor.forClass(NewTask.class);
        verify(createTask).create(eq(project.id()), captor.capture(), eq(owner));
        assertThat(captor.getValue()).isEqualTo(new NewTask("Escrever testes", "Fatia web", TaskPriority.HIGH, LocalDate.of(2026, 12, 1)));
    }

    @Test
    @DisplayName("corpo inválido para no controller, sem chamar o caso de uso")
    void validation() throws Exception {
        mvc.perform(post("/api/v1/projects/{id}/tasks", project.id().value())
                        .with(TestUsers.as(owner.value()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problems/validation"));

        verifyNoInteractions(createTask);
    }

    @Test
    @DisplayName("sem access token, a requisição é recusada com 401 antes de chegar ao controller")
    void missingToken() throws Exception {
        mvc.perform(get("/api/v1/projects/{id}/tasks", project.id().value()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(listTasks);
    }

    @Test
    @DisplayName("GET converte filtros, página e ordenação para os tipos da aplicação")
    void listMapsQueryParameters() throws Exception {
        UUID assignee = UUID.randomUUID();
        when(listTasks.list(any(), any(), any(), any())).thenReturn(new Page<>(List.of(task()), 1, 5, 6));

        mvc.perform(get("/api/v1/projects/{id}/tasks", project.id().value())
                        .with(TestUsers.as(owner.value()))
                        .param("status", "TODO")
                        .param("priority", "HIGH")
                        .param("assigneeId", assignee.toString())
                        .param("dueTo", "2026-12-31")
                        .param("page", "1")
                        .param("size", "5")
                        .param("sort", "dueDate,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Escrever testes"))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasNext").value(false));

        verify(listTasks).list(
                new ProjectId(project.id().value()),
                new Filter(TaskStatus.TODO, TaskPriority.HIGH, new UserId(assignee), null, LocalDate.of(2026, 12, 31)),
                new PageRequest(1, 5, SortField.DUE_DATE, false),
                owner);
    }

    @Test
    @DisplayName("ordenação desconhecida vira 400 antes de chegar ao caso de uso")
    void unknownSort() throws Exception {
        mvc.perform(get("/api/v1/projects/{id}/tasks", project.id().value())
                        .with(TestUsers.as(owner.value()))
                        .param("sort", "priority"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problems/invalid-value"));

        verifyNoInteractions(listTasks);
    }

    @Test
    @DisplayName("exceções do domínio viram o status certo: 422 para transição, 403 para quem não é membro")
    void domainExceptions() throws Exception {
        TaskId id = TaskId.newId();
        when(moveTask.move(id, TaskStatus.DONE, owner)).thenThrow(new InvalidTaskTransitionException(TaskStatus.TODO, TaskStatus.DONE));
        UserId stranger = UserId.newId();
        when(moveTask.move(id, TaskStatus.IN_PROGRESS, stranger)).thenThrow(new NotProjectMemberException(stranger, project.id()));

        mvc.perform(patch("/api/v1/tasks/{id}/status", id.value())
                        .with(TestUsers.as(owner.value()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"DONE\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.type").value("/problems/invalid-transition"));

        mvc.perform(patch("/api/v1/tasks/{id}/status", id.value())
                        .with(TestUsers.as(stranger.value()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"IN_PROGRESS\"}"))
                .andExpect(status().isForbidden());
    }
}
