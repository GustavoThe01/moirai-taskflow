package dev.gustavo.moirai.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Task")
class TaskTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-03T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 3);

    private final UserId owner = UserId.newId();
    private final UserId ana = UserId.newId();
    private final UserId outsider = UserId.newId();
    private Project project;

    @BeforeEach
    void setUp() {
        project = Project.create("Moirai", owner);
        project.addMember(ana, owner);
    }

    private Task newTask() {
        return Task.create(project, owner, "Criar o domínio", "Dia 02", TaskPriority.HIGH, TODAY.plusDays(7), CLOCK);
    }

    private Task taskInProgressBy(UserId assignee) {
        Task task = newTask();
        task.assignTo(assignee, owner, project);
        task.moveTo(TaskStatus.IN_PROGRESS, assignee, project);
        return task;
    }

    @Nested
    @DisplayName("ao criar")
    class Creation {

        @Test
        @DisplayName("nasce em TODO, sem responsável e com a data do relógio")
        void defaults() {
            Task task = newTask();
            assertThat(task.status()).isEqualTo(TaskStatus.TODO);
            assertThat(task.assigneeId()).isEmpty();
            assertThat(task.createdAt()).isEqualTo(CLOCK.instant());
            assertThat(task.projectId()).isEqualTo(project.id());
        }

        @Test
        @DisplayName("aceita prazo para hoje")
        void dueDateTodayIsValid() {
            Task task = Task.create(project, owner, "Revisar PR", null, TaskPriority.LOW, TODAY, CLOCK);
            assertThat(task.dueDate()).contains(TODAY);
        }

        @Test
        @DisplayName("rejeita prazo no passado")
        void rejectsPastDueDate() {
            assertThatThrownBy(() ->
                    Task.create(project, owner, "Tarefa", null, TaskPriority.LOW, TODAY.minusDays(1), CLOCK))
                    .isInstanceOf(InvalidValueException.class)
                    .hasMessageContaining("passado");
        }

        @Test
        @DisplayName("quem não é membro não cria tarefa")
        void outsiderCannotCreate() {
            assertThatThrownBy(() ->
                    Task.create(project, outsider, "Tarefa", null, TaskPriority.LOW, null, CLOCK))
                    .isInstanceOf(NotProjectMemberException.class);
        }

        @Test
        @DisplayName("rejeita título curto")
        void rejectsShortTitle() {
            assertThatThrownBy(() -> Task.create(project, owner, "ab", null, TaskPriority.LOW, null, CLOCK))
                    .isInstanceOf(InvalidValueException.class);
        }
    }

    @Nested
    @DisplayName("ao atribuir")
    class Assignment {

        @Test
        @DisplayName("um membro pode ser responsável")
        void assignsMember() {
            Task task = newTask();
            task.assignTo(ana, owner, project);
            assertThat(task.assigneeId()).contains(ana);
        }

        @Test
        @DisplayName("quem não é membro não pode ser responsável")
        void cannotAssignOutsider() {
            Task task = newTask();
            assertThatThrownBy(() -> task.assignTo(outsider, owner, project))
                    .isInstanceOf(NotProjectMemberException.class);
        }

        @Test
        @DisplayName("a tarefa não aceita um projeto diferente do seu")
        void rejectsOtherProject() {
            Task task = newTask();
            Project other = Project.create("Outro projeto", owner);
            assertThatThrownBy(() -> task.assignTo(owner, owner, other))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }
    }

    @Nested
    @DisplayName("ao mover")
    class Moving {

        @Test
        @DisplayName("não inicia sem responsável")
        void cannotStartWithoutAssignee() {
            Task task = newTask();
            assertThatThrownBy(() -> task.moveTo(TaskStatus.IN_PROGRESS, owner, project))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("responsável");
        }

        @Test
        @DisplayName("percorre o fluxo completo até DONE")
        void fullFlow() {
            Task task = taskInProgressBy(ana);
            task.moveTo(TaskStatus.REVIEW, ana, project);
            task.moveTo(TaskStatus.DONE, owner, project);
            assertThat(task.status()).isEqualTo(TaskStatus.DONE);
        }

        @Test
        @DisplayName("não pula etapas (TODO direto para DONE)")
        void cannotSkipSteps() {
            Task task = newTask();
            assertThatThrownBy(() -> task.moveTo(TaskStatus.DONE, owner, project))
                    .isInstanceOf(InvalidTaskTransitionException.class);
        }

        @Test
        @DisplayName("só o responsável envia para revisão")
        void onlyAssigneeSendsToReview() {
            Task task = taskInProgressBy(ana);
            assertThatThrownBy(() -> task.moveTo(TaskStatus.REVIEW, owner, project))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("responsável");
        }

        @Test
        @DisplayName("quem não é membro não move a tarefa")
        void outsiderCannotMove() {
            Task task = taskInProgressBy(ana);
            assertThatThrownBy(() -> task.moveTo(TaskStatus.TODO, outsider, project))
                    .isInstanceOf(NotProjectMemberException.class);
        }

        @Test
        @DisplayName("tarefa concluída não pode mais ser alterada")
        void doneIsFrozen() {
            Task task = taskInProgressBy(ana);
            task.moveTo(TaskStatus.REVIEW, ana, project);
            task.moveTo(TaskStatus.DONE, owner, project);

            assertThatThrownBy(() -> task.changePriority(TaskPriority.LOW))
                    .isInstanceOf(BusinessRuleViolationException.class);
            assertThatThrownBy(() -> task.moveTo(TaskStatus.IN_PROGRESS, owner, project))
                    .isInstanceOf(InvalidTaskTransitionException.class);
        }
    }

    @Nested
    @DisplayName("prazo")
    class DueDate {

        @Test
        @DisplayName("fica atrasada quando o prazo passa e ela não foi concluída")
        void becomesOverdue() {
            Task task = newTask();
            Clock nextMonth = Clock.offset(CLOCK, Duration.ofDays(30));
            assertThat(task.isOverdue(CLOCK)).isFalse();
            assertThat(task.isOverdue(nextMonth)).isTrue();
        }

        @Test
        @DisplayName("tarefa sem prazo nunca está atrasada")
        void noDueDateNeverOverdue() {
            Task task = Task.create(project, owner, "Sem prazo", null, TaskPriority.LOW, null, CLOCK);
            assertThat(task.isOverdue(Clock.offset(CLOCK, Duration.ofDays(365)))).isFalse();
        }

        @Test
        @DisplayName("reagendar para o passado é rejeitado")
        void rescheduleToPast() {
            Task task = newTask();
            assertThatThrownBy(() -> task.reschedule(TODAY.minusDays(2), CLOCK))
                    .isInstanceOf(InvalidValueException.class);
        }
    }
}
