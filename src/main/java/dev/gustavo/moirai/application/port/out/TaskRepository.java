package dev.gustavo.moirai.application.port.out;

import dev.gustavo.moirai.domain.ProjectId;
import dev.gustavo.moirai.domain.Task;
import dev.gustavo.moirai.domain.TaskId;
import java.util.List;
import java.util.Optional;

/** Porta de saída: como a aplicação guarda e recupera tarefas. */
public interface TaskRepository {

    void save(Task task);

    Optional<Task> findById(TaskId id);

    List<Task> findByProject(ProjectId projectId);
}
