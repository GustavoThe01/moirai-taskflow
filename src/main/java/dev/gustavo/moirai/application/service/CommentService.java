package dev.gustavo.moirai.application.service;

import dev.gustavo.moirai.application.port.in.CommentUseCases.AddComment;
import dev.gustavo.moirai.application.port.in.CommentUseCases.ListComments;
import dev.gustavo.moirai.application.port.out.CommentRepository;
import dev.gustavo.moirai.application.port.out.ProjectRepository;
import dev.gustavo.moirai.application.port.out.TaskRepository;
import dev.gustavo.moirai.domain.Comment;
import dev.gustavo.moirai.domain.Project;
import dev.gustavo.moirai.domain.ProjectNotFoundException;
import dev.gustavo.moirai.domain.Task;
import dev.gustavo.moirai.domain.TaskId;
import dev.gustavo.moirai.domain.TaskNotFoundException;
import dev.gustavo.moirai.domain.UserId;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CommentService implements AddComment, ListComments {

    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final CommentRepository comments;
    private final Clock clock;

    CommentService(ProjectRepository projects, TaskRepository tasks, CommentRepository comments, Clock clock) {
        this.projects = projects;
        this.tasks = tasks;
        this.comments = comments;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Comment add(TaskId taskId, String text, UserId author) {
        Task task = loadTask(taskId);
        Comment comment = Comment.write(task, loadProject(task), author, text, clock);
        comments.save(comment);
        return comment;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comment> list(TaskId taskId, UserId actor) {
        Task task = loadTask(taskId);
        loadProject(task).requireMember(actor);
        return comments.findByTask(taskId);
    }

    private Task loadTask(TaskId id) {
        return tasks.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
    }

    private Project loadProject(Task task) {
        return projects.findById(task.projectId()).orElseThrow(() -> new ProjectNotFoundException(task.projectId()));
    }
}
