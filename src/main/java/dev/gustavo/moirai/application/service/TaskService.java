package dev.gustavo.moirai.application.service;

import dev.gustavo.moirai.application.port.in.TaskUseCases.AssignTask;
import dev.gustavo.moirai.application.port.in.TaskUseCases.CreateTask;
import dev.gustavo.moirai.application.port.in.TaskUseCases.ListTasks;
import dev.gustavo.moirai.application.port.in.TaskUseCases.MoveTask;
import dev.gustavo.moirai.application.port.in.TaskUseCases.NewTask;
import dev.gustavo.moirai.application.port.out.ProjectRepository;
import dev.gustavo.moirai.application.port.out.TaskRepository;
import dev.gustavo.moirai.application.query.TaskQuery.Filter;
import dev.gustavo.moirai.application.query.TaskQuery.Page;
import dev.gustavo.moirai.application.query.TaskQuery.PageRequest;
import dev.gustavo.moirai.domain.Project;
import dev.gustavo.moirai.domain.ProjectId;
import dev.gustavo.moirai.domain.ProjectNotFoundException;
import dev.gustavo.moirai.domain.Task;
import dev.gustavo.moirai.domain.TaskId;
import dev.gustavo.moirai.domain.TaskNotFoundException;
import dev.gustavo.moirai.domain.TaskStatus;
import dev.gustavo.moirai.domain.UserId;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orquestra os casos de uso de tarefas. Quem decide se a operação é válida é o domínio. */
@Service
class TaskService implements CreateTask, MoveTask, AssignTask, ListTasks {

    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final Clock clock;

    TaskService(ProjectRepository projects, TaskRepository tasks, Clock clock) {
        this.projects = projects;
        this.tasks = tasks;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Task create(ProjectId projectId, NewTask newTask, UserId actor) {
        Task task = Task.create(loadProject(projectId), actor, newTask.title(), newTask.description(),
                newTask.priority(), newTask.dueDate(), clock);
        tasks.save(task);
        return task;
    }

    @Override
    @Transactional
    public Task move(TaskId id, TaskStatus target, UserId actor) {
        Task task = loadTask(id);
        task.moveTo(target, actor, loadProject(task.projectId()));
        tasks.save(task);
        return task;
    }

    @Override
    @Transactional
    public Task assign(TaskId id, UserId assignee, UserId actor) {
        Task task = loadTask(id);
        task.assignTo(assignee, actor, loadProject(task.projectId()));
        tasks.save(task);
        return task;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Task> list(ProjectId projectId, Filter filter, PageRequest page, UserId actor) {
        loadProject(projectId).requireMember(actor);
        return tasks.search(projectId, filter, page);
    }

    private Project loadProject(ProjectId id) {
        return projects.findById(id).orElseThrow(() -> new ProjectNotFoundException(id));
    }

    private Task loadTask(TaskId id) {
        return tasks.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }
}
