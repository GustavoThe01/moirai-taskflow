package dev.gustavo.moirai.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("TaskStatus")
class TaskStatusTest {

    @ParameterizedTest(name = "{0} -> {1} permitido = {2}")
    @CsvSource({
            "TODO, IN_PROGRESS, true",
            "TODO, REVIEW, false",
            "TODO, DONE, false",
            "IN_PROGRESS, TODO, true",
            "IN_PROGRESS, REVIEW, true",
            "IN_PROGRESS, DONE, false",
            "REVIEW, IN_PROGRESS, true",
            "REVIEW, DONE, true",
            "REVIEW, TODO, false",
            "DONE, TODO, false",
            "DONE, IN_PROGRESS, false",
            "DONE, REVIEW, false"
    })
    @DisplayName("segue a tabela de transições")
    void transitionTable(TaskStatus from, TaskStatus to, boolean allowed) {
        assertThat(from.canTransitionTo(to)).isEqualTo(allowed);
    }

    @Test
    @DisplayName("nenhum status transiciona para ele mesmo")
    void noSelfTransition() {
        for (TaskStatus status : TaskStatus.values()) {
            assertThat(status.canTransitionTo(status)).as(status.name()).isFalse();
        }
    }

    @Test
    @DisplayName("só DONE é final")
    void onlyDoneIsFinal() {
        assertThat(TaskStatus.DONE.isFinal()).isTrue();
        assertThat(TaskStatus.REVIEW.isFinal()).isFalse();
    }
}
