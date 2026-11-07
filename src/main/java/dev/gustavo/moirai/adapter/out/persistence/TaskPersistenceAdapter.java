package dev.gustavo.moirai.adapter.out.persistence;

import dev.gustavo.moirai.application.port.out.TaskRepository;
import dev.gustavo.moirai.application.query.TaskQuery.Filter;
import dev.gustavo.moirai.application.query.TaskQuery.Page;
import dev.gustavo.moirai.application.query.TaskQuery.PageRequest;
import dev.gustavo.moirai.domain.ProjectId;
import dev.gustavo.moirai.domain.Task;
import dev.gustavo.moirai.domain.TaskId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class TaskPersistenceAdapter implements TaskRepository {

    private final TaskJpaRepository tasks;

    TaskPersistenceAdapter(TaskJpaRepository tasks) {
        this.tasks = tasks;
    }

    @Override
    @Transactional
    public void save(Task task) {
        tasks.save(PersistenceMapper.toEntity(task));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Task> findById(TaskId id) {
        return tasks.findById(id.toString()).map(PersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Task> findByProject(ProjectId projectId) {
        return tasks.findByProjectIdOrderByCreatedAt(projectId.toString()).stream()
                .map(PersistenceMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Task> search(ProjectId projectId, Filter filter, PageRequest page) {
        var result = tasks.findAll(
                TaskSpecifications.matching(projectId, filter),
                org.springframework.data.domain.PageRequest.of(page.page(), page.size(), sortOf(page)));
        return new Page<>(
                result.getContent().stream().map(PersistenceMapper::toDomain).toList(),
                page.page(),
                page.size(),
                result.getTotalElements());
    }

    /** Ordena pelo campo pedido e desempata pelo id, para a paginação não repetir nem pular linhas. */
    private static Sort sortOf(PageRequest page) {
        String attribute = switch (page.sort()) {
            case CREATED_AT -> "createdAt";
            case DUE_DATE -> "dueDate";
            case TITLE -> "title";
        };
        Sort.Direction direction = page.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, attribute).and(Sort.by(Sort.Direction.ASC, "id"));
    }
}
