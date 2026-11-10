package dev.gustavo.moirai.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Etiquetas e comentários")
class LabelAndCommentTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneOffset.UTC);

    private final UserId owner = UserId.newId();
    private final UserId stranger = UserId.newId();
    private final Project project = Project.create("Moirai", owner);

    private Task task() {
        return Task.create(project, owner, "Revisar", null, TaskPriority.LOW, null, CLOCK);
    }

    @Test
    @DisplayName("a etiqueta normaliza a cor e exige membro do projeto")
    void createsLabel() {
        Label label = Label.create(project, owner, "  bug ", "#e11d48");

        assertThat(label.name()).isEqualTo("bug");
        assertThat(label.color()).isEqualTo("#E11D48");
        assertThatThrownBy(() -> Label.create(project, stranger, "bug", "#E11D48")).isInstanceOf(NotProjectMemberException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"vermelho", "#FFF", "E11D48", "#GG0000"})
    @DisplayName("cor fora do formato #RRGGBB é recusada")
    void rejectsColor(String color) {
        assertThatThrownBy(() -> Label.create(project, owner, "bug", color)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("nome vazio ou com mais de 30 caracteres é recusado")
    void rejectsName() {
        assertThatThrownBy(() -> Label.create(project, owner, " ", "#E11D48")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> Label.create(project, owner, "x".repeat(31), "#E11D48")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("relabel troca o conjunto inteiro, e lista vazia remove todas")
    void relabel() {
        Task task = task();
        Label bug = Label.create(project, owner, "bug", "#E11D48");
        Label urgent = Label.create(project, owner, "urgente", "#F59E0B");

        task.relabel(List.of(bug, urgent), owner, project);
        assertThat(task.labels()).containsExactlyInAnyOrder(bug.id(), urgent.id());

        task.relabel(List.of(), owner, project);
        assertThat(task.labels()).isEmpty();
    }

    @Test
    @DisplayName("etiqueta de outro projeto e mais de 10 etiquetas são regras de negócio violadas")
    void relabelRules() {
        Task task = task();
        Project other = Project.create("Outro", owner);
        List<Label> eleven = IntStream.range(0, 11).mapToObj(i -> Label.create(project, owner, "e" + i, "#000000")).toList();

        assertThatThrownBy(() -> task.relabel(List.of(Label.create(other, owner, "bug", "#E11D48")), owner, project))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("outro projeto");
        assertThatThrownBy(() -> task.relabel(eleven, owner, project))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("10");
        assertThatThrownBy(() -> task.relabel(List.of(), stranger, project)).isInstanceOf(NotProjectMemberException.class);
    }

    @Test
    @DisplayName("comentário registra autor e horário, e só membros comentam")
    void comment() {
        Task task = task();

        Comment comment = Comment.write(task, project, owner, "  Pronto para revisão. ", CLOCK);

        assertThat(comment.text()).isEqualTo("Pronto para revisão.");
        assertThat(comment.createdAt()).isEqualTo(CLOCK.instant());
        assertThatThrownBy(() -> Comment.write(task, project, stranger, "Oi", CLOCK)).isInstanceOf(NotProjectMemberException.class);
        assertThatThrownBy(() -> Comment.write(task, project, owner, " ", CLOCK)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> Comment.write(task, project, owner, "x".repeat(2001), CLOCK)).isInstanceOf(InvalidValueException.class);
    }
}
