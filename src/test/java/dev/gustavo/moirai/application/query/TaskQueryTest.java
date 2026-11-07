package dev.gustavo.moirai.application.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.gustavo.moirai.application.query.TaskQuery.Filter;
import dev.gustavo.moirai.application.query.TaskQuery.Page;
import dev.gustavo.moirai.application.query.TaskQuery.PageRequest;
import dev.gustavo.moirai.application.query.TaskQuery.SortField;
import dev.gustavo.moirai.domain.InvalidValueException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Tipos da consulta de tarefas")
class TaskQueryTest {

    @Test
    @DisplayName("a página calcula total de páginas e se existe próxima")
    void pageTotals() {
        Page<String> page = new Page<>(List.of("a", "b"), 1, 2, 5);

        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.hasNext()).isTrue();
        assertThat(new Page<>(List.of("e"), 2, 2, 5).hasNext()).isFalse();
        assertThat(new Page<String>(List.of(), 0, 20, 0).totalPages()).isZero();
    }

    @Test
    @DisplayName("map converte os itens e mantém a paginação")
    void pageMap() {
        Page<Integer> lengths = new Page<>(List.of("abc", "de"), 0, 2, 3).map(String::length);

        assertThat(lengths.items()).containsExactly(3, 2);
        assertThat(lengths.totalItems()).isEqualTo(3);
    }

    @ParameterizedTest
    @CsvSource({"-1, 20", "0, 0", "0, 101"})
    @DisplayName("página negativa ou tamanho fora de 1 a 100 é recusado")
    void invalidPageRequest(int page, int size) {
        assertThatThrownBy(() -> new PageRequest(page, size, SortField.TITLE, true))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("intervalo de prazo invertido é recusado")
    void invertedDueRange() {
        LocalDate today = LocalDate.of(2026, 11, 7);

        assertThatThrownBy(() -> new Filter(null, null, null, today, today.minusDays(1)))
                .isInstanceOf(InvalidValueException.class);
        assertThat(new Filter(null, null, null, today, today).dueTo()).isEqualTo(today);
    }
}
