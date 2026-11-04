package dev.gustavo.moirai.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.gustavo.moirai.application.port.out.ProjectRepository;
import dev.gustavo.moirai.application.port.out.TaskRepository;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ActiveProfiles;

/** Testa a porta contra o banco de verdade: migrations do Flyway, mapeamento JPA e SQLite. */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Adapters de persistência")
class PersistenceAdapterIntegrationTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-04T12:00:00.123Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);

    @Autowired
    private ProjectRepository projects;

    @Autowired
    private TaskRepository tasks;

    private final UserId owner = UserId.newId();
    private final UserId ana = UserId.newId();

    private Project savedProject() {
        Project project = Project.create("Moirai", owner);
        project.addMember(ana, owner);
        projects.save(project);
        return project;
    }

    @Test
    @DisplayName("salva e recupera o projeto com os membros")
    void projectRoundTrip() {
        Project project = savedProject();

        Project found = projects.findById(project.id()).orElseThrow();

        assertThat(found.name()).isEqualTo("Moirai");
        assertThat(found.ownerId()).isEqualTo(owner);
        assertThat(found.members()).containsExactlyInAnyOrder(owner, ana);
    }

    @Test
    @DisplayName("atualizar o projeto persiste a remoção de membro")
    void projectUpdate() {
        Project project = savedProject();

        project.removeMember(ana, owner);
        projects.save(project);

        assertThat(projects.findById(project.id()).orElseThrow().members()).containsExactly(owner);
    }

    @Test
    @DisplayName("devolve vazio para id inexistente")
    void missingIds() {
        assertThat(projects.findById(ProjectId.newId())).isEmpty();
        assertThat(tasks.findById(TaskId.newId())).isEmpty();
    }

    @Test
    @DisplayName("salva e recupera a tarefa com todos os campos")
    void taskRoundTrip() {
        Project project = savedProject();
        Task task = Task.create(project, owner, "Persistir", "Dia 03", TaskPriority.URGENT, TODAY.plusDays(2), CLOCK);
        task.assignTo(ana, owner, project);
        task.moveTo(TaskStatus.IN_PROGRESS, ana, project);
        tasks.save(task);

        Task found = tasks.findById(task.id()).orElseThrow();

        assertThat(found.projectId()).isEqualTo(project.id());
        assertThat(found.createdBy()).isEqualTo(owner);
        assertThat(found.createdAt()).isEqualTo(CLOCK.instant());
        assertThat(found.title()).isEqualTo("Persistir");
        assertThat(found.description()).isEqualTo("Dia 03");
        assertThat(found.priority()).isEqualTo(TaskPriority.URGENT);
        assertThat(found.dueDate()).contains(TODAY.plusDays(2));
        assertThat(found.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(found.assigneeId()).contains(ana);
    }

    @Test
    @DisplayName("tarefa sem prazo e sem responsável volta com os opcionais vazios")
    void taskOptionalFields() {
        Project project = savedProject();
        Task task = Task.create(project, owner, "Sem prazo", null, TaskPriority.LOW, null, CLOCK);
        tasks.save(task);

        Task found = tasks.findById(task.id()).orElseThrow();

        assertThat(found.dueDate()).isEmpty();
        assertThat(found.assigneeId()).isEmpty();
        assertThat(found.description()).isEmpty();
    }

    @Test
    @DisplayName("lista só as tarefas do projeto pedido")
    void tasksByProject() {
        Project project = savedProject();
        Project other = savedProject();
        Task mine = Task.create(project, owner, "Minha", null, TaskPriority.LOW, null, CLOCK);
        Task theirs = Task.create(other, owner, "De outro", null, TaskPriority.LOW, null, CLOCK);
        tasks.save(mine);
        tasks.save(theirs);

        assertThat(tasks.findByProject(project.id())).containsExactly(mine);
    }

    @Test
    @DisplayName("a chave estrangeira impede tarefa de um projeto que não foi salvo")
    void foreignKeyIsEnforced() {
        Project neverSaved = Project.create("Fantasma", owner);
        Task task = Task.create(neverSaved, owner, "Órfã", null, TaskPriority.LOW, null, CLOCK);

        assertThatThrownBy(() -> tasks.save(task))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("FOREIGN KEY");
    }
}
