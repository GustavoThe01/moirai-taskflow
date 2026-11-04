package dev.gustavo.moirai.adapter.out.persistence;

import dev.gustavo.moirai.application.port.out.TaskRepository;
import dev.gustavo.moirai.domain.ProjectId;
import dev.gustavo.moirai.domain.Task;
import dev.gustavo.moirai.domain.TaskId;
import java.util.List;
import java.util.Optional;
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
}
