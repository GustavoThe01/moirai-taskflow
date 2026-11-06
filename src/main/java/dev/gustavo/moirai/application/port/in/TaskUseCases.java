package dev.gustavo.moirai.application.port.in;

import dev.gustavo.moirai.domain.ProjectId;
import dev.gustavo.moirai.domain.Task;
import dev.gustavo.moirai.domain.TaskId;
import dev.gustavo.moirai.domain.TaskPriority;
import dev.gustavo.moirai.domain.TaskStatus;
import dev.gustavo.moirai.domain.UserId;
import java.time.LocalDate;

/** Portas de entrada: o que o mundo externo pode pedir à aplicação sobre tarefas. */
public final class TaskUseCases {

    private TaskUseCases() {
    }

    public record NewTask(String title, String description, TaskPriority priority, LocalDate dueDate) {
    }

    public interface CreateTask {
        Task create(ProjectId projectId, NewTask newTask, UserId actor);
    }

    public interface MoveTask {
        Task move(TaskId id, TaskStatus target, UserId actor);
    }

    public interface AssignTask {
        Task assign(TaskId id, UserId assignee, UserId actor);
    }
}
