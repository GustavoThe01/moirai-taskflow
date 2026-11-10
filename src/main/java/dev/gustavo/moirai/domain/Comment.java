package dev.gustavo.moirai.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Comentário numa tarefa. É imutável: o histórico da conversa não muda depois de escrito.
 *
 * <p>Só membros do projeto comentam, e uma tarefa concluída ainda aceita comentários
 * (é comum registrar algo depois de fechar).
 */
public final class Comment {

    private static final int TEXT_MAX_LENGTH = 2000;

    private final CommentId id;
    private final TaskId taskId;
    private final UserId author;
    private final String text;
    private final Instant createdAt;

    private Comment(CommentId id, TaskId taskId, UserId author, String text, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.taskId = Objects.requireNonNull(taskId, "taskId");
        this.author = Objects.requireNonNull(author, "author");
        this.text = validateText(text);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static Comment write(Task task, Project project, UserId author, String text, Clock clock) {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(project, "project");
        if (!task.projectId().equals(project.id())) {
            throw new BusinessRuleViolationException("A tarefa %s não pertence ao projeto %s.".formatted(task.id(), project.id()));
        }
        project.requireMember(author);
        return new Comment(CommentId.newId(), task.id(), author, text, clock.instant());
    }

    public static Comment restore(CommentId id, TaskId taskId, UserId author, String text, Instant createdAt) {
        return new Comment(id, taskId, author, text, createdAt);
    }

    public CommentId id() {
        return id;
    }

    public TaskId taskId() {
        return taskId;
    }

    public UserId author() {
        return author;
    }

    public String text() {
        return text;
    }

    public Instant createdAt() {
        return createdAt;
    }

    private static String validateText(String text) {
        if (text == null || text.isBlank()) {
            throw new InvalidValueException("O comentário não pode ficar vazio.");
        }
        String stripped = text.strip();
        if (stripped.length() > TEXT_MAX_LENGTH) {
            throw new InvalidValueException("O comentário tem no máximo %d caracteres.".formatted(TEXT_MAX_LENGTH));
        }
        return stripped;
    }
}
