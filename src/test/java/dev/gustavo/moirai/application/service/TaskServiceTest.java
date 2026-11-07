package dev.gustavo.moirai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.gustavo.moirai.application.port.in.TaskUseCases.NewTask;
import dev.gustavo.moirai.application.port.out.ProjectRepository;
import dev.gustavo.moirai.application.port.out.TaskRepository;
import dev.gustavo.moirai.application.query.TaskQuery.Filter;
import dev.gustavo.moirai.application.query.TaskQuery.Page;
import dev.gustavo.moirai.application.query.TaskQuery.PageRequest;
import dev.gustavo.moirai.domain.InvalidTaskTransitionException;
import dev.gustavo.moirai.domain.NotProjectMemberException;
import dev.gustavo.moirai.domain.Project;
import dev.gustavo.moirai.domain.ProjectId;
import dev.gustavo.moirai.domain.ProjectNotFoundException;
import dev.gustavo.moirai.domain.Task;
import dev.gustavo.moirai.domain.TaskId;
import dev.gustavo.moirai.domain.TaskNotFoundException;
import dev.gustavo.moirai.domain.TaskPriority;
import dev.gustavo.moirai.domain.TaskStatus;
import dev.gustavo.moirai.domain.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TaskService")
class TaskServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-06T12:00:00Z"), ZoneOffset.UTC);
    private static final NewTask NEW_TASK = new NewTask("Revisar", null, TaskPriority.LOW, null);

    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final TaskRepository tasks = mock(TaskRepository.class);
    private final TaskService service = new TaskService(projects, tasks, CLOCK);
    private final UserId owner = UserId.newId();
    private final UserId ana = UserId.newId();
    private Project project;

    @BeforeEach
    void setUp() {
        project = Project.create("Moirai", owner);
        project.addMember(ana, owner);
        when(projects.findById(project.id())).thenReturn(Optional.of(project));
    }

    private Task existingTask() {
        Task task = Task.create(project, owner, "Revisar", null, TaskPriority.LOW, null, CLOCK);
        when(tasks.findById(task.id())).thenReturn(Optional.of(task));
        return task;
    }

    @Test
    @DisplayName("criar usa o relógio e salva")
    void createsWithClock() {
        Task task = service.create(project.id(), NEW_TASK, owner);

        assertThat(task.createdAt()).isEqualTo(CLOCK.instant());
        verify(tasks).save(task);
    }

    @Test
    @DisplayName("criar em projeto inexistente é recusado")
    void createInMissingProject() {
        ProjectId missing = ProjectId.newId();
        when(projects.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(missing, NEW_TASK, owner))
                .isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    @DisplayName("atribuir a outro membro e iniciar salva o novo estado")
    void assignsAndMoves() {
        Task task = existingTask();

        service.assign(task.id(), ana, owner);
        Task moved = service.move(task.id(), TaskStatus.IN_PROGRESS, ana);

        assertThat(moved.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(moved.assigneeId()).contains(ana);
    }

    @Test
    @DisplayName("transição inválida não salva nada")
    void invalidMoveSavesNothing() {
        Task task = existingTask();

        assertThatThrownBy(() -> service.move(task.id(), TaskStatus.DONE, owner))
                .isInstanceOf(InvalidTaskTransitionException.class);
        verify(tasks, never()).save(any());
    }

    @Test
    @DisplayName("tarefa inexistente é recusada")
    void missingTask() {
        TaskId missing = TaskId.newId();
        when(tasks.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.move(missing, TaskStatus.IN_PROGRESS, owner))
                .isInstanceOf(TaskNotFoundException.class);
    }

    @Test
    @DisplayName("membro lista as tarefas pela porta de saída")
    void listForMember() {
        Page<Task> page = new Page<>(List.of(existingTask()), 0, 20, 1);
        when(tasks.search(project.id(), Filter.NONE, PageRequest.first(20))).thenReturn(page);

        assertThat(service.list(project.id(), Filter.NONE, PageRequest.first(20), ana)).isSameAs(page);
    }

    @Test
    @DisplayName("quem não é membro não chega a consultar o banco")
    void listForStranger() {
        assertThatThrownBy(() -> service.list(project.id(), Filter.NONE, PageRequest.first(20), UserId.newId()))
                .isInstanceOf(NotProjectMemberException.class);

        verify(tasks, never()).search(any(), any(), any());
    }
}
