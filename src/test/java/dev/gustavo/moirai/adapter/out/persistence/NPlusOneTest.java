package dev.gustavo.moirai.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.gustavo.moirai.application.port.out.LabelRepository;
import dev.gustavo.moirai.application.port.out.ProjectRepository;
import dev.gustavo.moirai.application.port.out.TaskRepository;
import dev.gustavo.moirai.application.query.TaskQuery.Filter;
import dev.gustavo.moirai.application.query.TaskQuery.PageRequest;
import dev.gustavo.moirai.domain.Label;
import dev.gustavo.moirai.domain.Project;
import dev.gustavo.moirai.domain.Task;
import dev.gustavo.moirai.domain.TaskPriority;
import dev.gustavo.moirai.domain.UserId;
import jakarta.persistence.EntityManagerFactory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Conta os comandos SQL de verdade, pelas estatísticas do Hibernate.
 *
 * <p>N+1 é quando listar N itens gera 1 consulta para a lista e mais 1 para cada item, para carregar uma coleção.
 * Com 20 tarefas por página, seriam 21 consultas em vez de 2 ou 3. O teste trava o número máximo de comandos:
 * se alguém mexer no mapeamento e o N+1 voltar, o teste quebra.
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@DisplayName("Consultas sem N+1")
class NPlusOneTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneOffset.UTC);

    @Autowired
    private ProjectRepository projects;

    @Autowired
    private TaskRepository tasks;

    @Autowired
    private LabelRepository labels;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Statistics statistics;

    @BeforeEach
    void enableStatistics() {
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
    }

    private long statementsDuring(Runnable action) {
        statistics.clear();
        action.run();
        return statistics.getPrepareStatementCount();
    }

    @Test
    @DisplayName("listar uma página de 20 tarefas com etiquetas não faz uma consulta por tarefa")
    void taskPageWithLabels() {
        UserId owner = UserId.newId();
        Project project = Project.create("N+1 das tarefas", owner);
        projects.save(project);
        Label bug = Label.create(project, owner, "bug", "#E11D48");
        Label urgent = Label.create(project, owner, "urgente", "#F59E0B");
        labels.save(bug);
        labels.save(urgent);
        for (int i = 0; i < 20; i++) {
            Task task = Task.create(project, owner, "Tarefa " + i, null, TaskPriority.LOW, null, CLOCK);
            task.relabel(List.of(bug, urgent), owner, project);
            tasks.save(task);
        }

        long statements = statementsDuring(() -> {
            var page = tasks.search(project.id(), Filter.NONE, PageRequest.first(20));
            assertThat(page.items()).hasSize(20).allSatisfy(task -> assertThat(task.labels()).hasSize(2));
        });

        // 1 para a página, 1 para o total e 1 para as etiquetas das 20 tarefas de uma vez.
        assertThat(statements).isLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("listar os projetos do usuário não faz uma consulta por projeto para os membros")
    void projectsWithMembers() {
        UserId member = UserId.newId();
        for (int i = 0; i < 15; i++) {
            Project project = Project.create("Projeto %02d".formatted(i), member);
            project.addMember(UserId.newId(), member);
            projects.save(project);
        }

        long statements = statementsDuring(() ->
                assertThat(projects.findByMember(member)).hasSize(15).allSatisfy(p -> assertThat(p.members()).hasSize(2)));

        assertThat(statements).isLessThanOrEqualTo(2);
    }
}
