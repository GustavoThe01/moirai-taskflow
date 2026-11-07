package dev.gustavo.moirai.application.query;

import dev.gustavo.moirai.domain.InvalidValueException;
import dev.gustavo.moirai.domain.TaskPriority;
import dev.gustavo.moirai.domain.TaskStatus;
import dev.gustavo.moirai.domain.UserId;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

/**
 * Tipos da consulta de tarefas, compartilhados pela porta de entrada e pela de saída.
 * Não dependem do Spring Data: o adapter de persistência é que traduz para {@code Pageable} e {@code Specification}.
 */
public final class TaskQuery {

    /** Maior página aceita. Evita que um cliente peça a tabela inteira de uma vez. */
    public static final int MAX_PAGE_SIZE = 100;

    private TaskQuery() {
    }

    /** Campos pelos quais a lista pode ser ordenada. */
    public enum SortField {
        CREATED_AT, DUE_DATE, TITLE
    }

    /** Filtros opcionais. Campo {@code null} significa "sem filtro". */
    public record Filter(TaskStatus status, TaskPriority priority, UserId assignee, LocalDate dueFrom, LocalDate dueTo) {

        public static final Filter NONE = new Filter(null, null, null, null, null);

        public Filter {
            if (dueFrom != null && dueTo != null && dueTo.isBefore(dueFrom)) {
                throw new InvalidValueException("O fim do intervalo de prazo não pode ser antes do início.");
            }
        }
    }

    /** Página pedida, começando em zero. */
    public record PageRequest(int page, int size, SortField sort, boolean ascending) {

        public PageRequest {
            if (page < 0) {
                throw new InvalidValueException("A página começa em 0.");
            }
            if (size < 1 || size > MAX_PAGE_SIZE) {
                throw new InvalidValueException("O tamanho da página deve ficar entre 1 e " + MAX_PAGE_SIZE + ".");
            }
            if (sort == null) {
                throw new InvalidValueException("Informe o campo de ordenação.");
            }
        }

        public static PageRequest first(int size) {
            return new PageRequest(0, size, SortField.CREATED_AT, true);
        }
    }

    /** Uma página de resultados e o total que existe no banco. */
    public record Page<T>(List<T> items, int page, int size, long totalItems) {

        public Page {
            items = List.copyOf(items);
        }

        public int totalPages() {
            return (int) ((totalItems + size - 1) / size);
        }

        public boolean hasNext() {
            return page + 1 < totalPages();
        }

        public <R> Page<R> map(Function<? super T, ? extends R> mapper) {
            return new Page<>(items.stream().<R>map(mapper).toList(), page, size, totalItems);
        }
    }
}
