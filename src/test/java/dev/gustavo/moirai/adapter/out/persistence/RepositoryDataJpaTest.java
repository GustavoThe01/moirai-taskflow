package dev.gustavo.moirai.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.gustavo.moirai.application.query.TaskQuery.Filter;
import dev.gustavo.moirai.application.query.TaskQuery.PageRequest;
import dev.gustavo.moirai.application.query.TaskQuery.SortField;
import dev.gustavo.moirai.domain.Project;
import dev.gustavo.moirai.domain.Task;
import dev.gustavo.moirai.domain.TaskPriority;
import dev.gustavo.moirai.domain.TaskStatus;
import dev.gustavo.moirai.domain.UserId;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Fatia de persistência: JPA, Flyway e o SQLite de teste, sem web e sem serviços.
 *
 * <p>{@code replace = NONE} mantém o SQLite com as migrations reais, em vez de um banco embutido diferente
 * do que a aplicação usa. Cada teste roda numa transação desfeita no fim, então os testes não se enxergam.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({ProjectPersistenceAdapter.class, TaskPersistenceAdapter.class, RepositoryDataJpaTest.FixedClock.class})
@DisplayName("Repositórios (@DataJpaTest)")
class RepositoryDataJpaTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-09T12:00:00Z"), ZoneOffset.UTC);

    /** A fatia JPA não carrega as configurações da aplicação; o adapter de projetos precisa de um relógio. */
    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return CLOCK;
        }
    }
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 9);

    @Autowired
    private ProjectPersistenceAdapter projects;

    @Autowired
    private TaskPersistenceAdapter tasks;

    @Autowired
    private ProjectJpaRepository projectRows;

    @Autowired
    private EntityManager entityManager;

    private final UserId ana = UserId.newId();
    private final UserId bruno = UserId.newId();

    private Project project(String name, UserId owner) {
        Project project = Project.create(name, owner);
        projects.save(project);
        return project;
    }

    @Test
    @DisplayName("a consulta JPQL de membros traz só os projetos do usuário, em ordem de nome")
    void findByMember() {
        project("Zeus", ana);
        Project shared = project("Apolo", bruno);
        shared.addMember(ana, bruno);
        projects.save(shared);
        project("Hermes", bruno);

        assertThat(projectRows.findByMember(ana.toString()))
                .extracting(ProjectJpaEntity::getName)
                .containsExactly("Apolo", "Zeus");
    }

    @Test
    @DisplayName("as Specifications combinam status, responsável e prazo numa consulta só")
    void specifications() {
        Project project = project("Moirai", ana);
        project.addMember(bruno, ana);
        projects.save(project);
        Task brunoSoon = Task.create(project, ana, "Do Bruno, logo", null, TaskPriority.HIGH, TODAY.plusDays(2), CLOCK);
        brunoSoon.assignTo(bruno, ana, project);
        Task brunoLate = Task.create(project, ana, "Do Bruno, depois", null, TaskPriority.HIGH, TODAY.plusDays(40), CLOCK);
        brunoLate.assignTo(bruno, ana, project);
        Task anaSoon = Task.create(project, ana, "Da Ana", null, TaskPriority.LOW, TODAY.plusDays(1), CLOCK);
        anaSoon.assignTo(ana, ana, project);
        List.of(brunoSoon, brunoLate, anaSoon).forEach(tasks::save);

        var result = tasks.search(project.id(),
                new Filter(TaskStatus.TODO, null, bruno, TODAY, TODAY.plusDays(30)),
                new PageRequest(0, 10, SortField.DUE_DATE, true));

        assertThat(result.items()).containsExactly(brunoSoon);
        assertThat(result.totalItems()).isEqualTo(1);
    }

    @Test
    @DisplayName("o Flyway aplicou as migrations, inclusive os índices da listagem")
    void migrations() {
        @SuppressWarnings("unchecked")
        List<String> indexes = entityManager
                .createNativeQuery("SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'task'")
                .getResultList();

        assertThat(indexes).contains("ix_task_project_status", "ix_task_project_due_date", "ix_task_project_assignee");
    }
}
