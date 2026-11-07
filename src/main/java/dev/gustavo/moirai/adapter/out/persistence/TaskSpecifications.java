package dev.gustavo.moirai.adapter.out.persistence;

import dev.gustavo.moirai.application.query.TaskQuery.Filter;
import dev.gustavo.moirai.domain.ProjectId;
import org.springframework.data.jpa.domain.Specification;

/**
 * Monta o WHERE da busca de tarefas a partir dos filtros preenchidos.
 * Cada filtro é uma {@link Specification} pequena; o que vier {@code null} simplesmente não entra na consulta.
 * Prazos ficam como texto ISO 8601 ({@code yyyy-MM-dd}), então comparar texto equivale a comparar datas.
 */
final class TaskSpecifications {

    private TaskSpecifications() {
    }

    static Specification<TaskJpaEntity> matching(ProjectId projectId, Filter filter) {
        Specification<TaskJpaEntity> spec = equalTo("projectId", projectId.toString());
        if (filter.status() != null) {
            spec = spec.and(equalTo("status", filter.status().name()));
        }
        if (filter.priority() != null) {
            spec = spec.and(equalTo("priority", filter.priority().name()));
        }
        if (filter.assignee() != null) {
            spec = spec.and(equalTo("assigneeId", filter.assignee().toString()));
        }
        if (filter.dueFrom() != null) {
            String from = filter.dueFrom().toString();
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dueDate"), from));
        }
        if (filter.dueTo() != null) {
            String to = filter.dueTo().toString();
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("dueDate"), to));
        }
        return spec;
    }

    private static Specification<TaskJpaEntity> equalTo(String attribute, String value) {
        return (root, query, cb) -> cb.equal(root.get(attribute), value);
    }
}
