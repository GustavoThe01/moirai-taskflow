package dev.gustavo.moirai.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Tarefa de um projeto. Guarda o fio que as Moiras tecem: de TODO até DONE.
 *
 * <p>Regras:
 * <ul>
 *   <li>só membros do projeto criam, atribuem e movem tarefas;</li>
 *   <li>as transições seguem {@link TaskStatus#allowedTransitions()};</li>
 *   <li>uma tarefa só sai de TODO para IN_PROGRESS se tiver responsável;</li>
 *   <li>só o responsável envia a tarefa para REVIEW;</li>
 *   <li>o prazo, quando existe, não pode estar no passado;</li>
 *   <li>tarefa em DONE não muda mais.</li>
 * </ul>
 *
 * <p>O {@link Clock} é recebido por parâmetro para que as regras de data sejam testáveis
 * sem depender do relógio da máquina.
 */
public final class Task {

    private static final int TITLE_MIN_LENGTH = 3;
    private static final int TITLE_MAX_LENGTH = 120;

    private final TaskId id;
    private final ProjectId projectId;
    private final UserId createdBy;
    private final Instant createdAt;
    private String title;
    private String description;
    private TaskPriority priority;
    private LocalDate dueDate;
    private TaskStatus status;
    private UserId assigneeId;

    private Task(TaskId id, ProjectId projectId, UserId createdBy, Instant createdAt,
                 String title, String description, TaskPriority priority, LocalDate dueDate) {
        this.id = id;
        this.projectId = projectId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.title = validateTitle(title);
        this.description = description == null ? "" : description.strip();
        this.priority = Objects.requireNonNull(priority, "priority");
        this.dueDate = dueDate;
        this.status = TaskStatus.TODO;
    }

    public static Task create(Project project, UserId creator, String title, String description,
                              TaskPriority priority, LocalDate dueDate, Clock clock) {
        Objects.requireNonNull(project, "project");
        project.requireMember(creator);
        validateDueDate(dueDate, clock);
        return new Task(TaskId.newId(), project.id(), creator, clock.instant(),
                title, description, priority, dueDate);
    }

    public void assignTo(UserId assignee, UserId actor, Project project) {
        requireSameProject(project);
        requireNotDone();
        project.requireMember(actor);
        project.requireMember(assignee);
        assigneeId = assignee;
    }

    public void moveTo(TaskStatus target, UserId actor, Project project) {
        Objects.requireNonNull(target, "target");
        requireSameProject(project);
        project.requireMember(actor);
        if (!status.canTransitionTo(target)) {
            throw new InvalidTaskTransitionException(status, target);
        }
        if (target == TaskStatus.IN_PROGRESS && assigneeId == null) {
            throw new BusinessRuleViolationException("Atribua um responsável antes de iniciar a tarefa.");
        }
        if (target == TaskStatus.REVIEW && !actor.equals(assigneeId)) {
            throw new BusinessRuleViolationException("Só o responsável pela tarefa pode enviá-la para revisão.");
        }
        status = target;
    }

    public void changePriority(TaskPriority newPriority) {
        requireNotDone();
        priority = Objects.requireNonNull(newPriority, "newPriority");
    }

    public void reschedule(LocalDate newDueDate, Clock clock) {
        requireNotDone();
        validateDueDate(newDueDate, clock);
        dueDate = newDueDate;
    }

    /** Atrasada: tem prazo, o prazo já passou e ainda não foi concluída. */
    public boolean isOverdue(Clock clock) {
        return dueDate != null && !status.isFinal() && dueDate.isBefore(LocalDate.now(clock));
    }

    public TaskId id() {
        return id;
    }

    public ProjectId projectId() {
        return projectId;
    }

    public UserId createdBy() {
        return createdBy;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public TaskPriority priority() {
        return priority;
    }

    public Optional<LocalDate> dueDate() {
        return Optional.ofNullable(dueDate);
    }

    public TaskStatus status() {
        return status;
    }

    public Optional<UserId> assigneeId() {
        return Optional.ofNullable(assigneeId);
    }

    private void requireSameProject(Project project) {
        Objects.requireNonNull(project, "project");
        if (!project.id().equals(projectId)) {
            throw new BusinessRuleViolationException("A tarefa %s não pertence ao projeto %s."
                    .formatted(id, project.id()));
        }
    }

    private void requireNotDone() {
        if (status.isFinal()) {
            throw new BusinessRuleViolationException("Uma tarefa concluída não pode ser alterada.");
        }
    }

    private static void validateDueDate(LocalDate dueDate, Clock clock) {
        Objects.requireNonNull(clock, "clock");
        if (dueDate != null && dueDate.isBefore(LocalDate.now(clock))) {
            throw new InvalidValueException("O prazo não pode estar no passado: " + dueDate);
        }
    }

    private static String validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new InvalidValueException("O título da tarefa é obrigatório.");
        }
        String stripped = title.strip();
        if (stripped.length() < TITLE_MIN_LENGTH || stripped.length() > TITLE_MAX_LENGTH) {
            throw new InvalidValueException("O título deve ter entre %d e %d caracteres."
                    .formatted(TITLE_MIN_LENGTH, TITLE_MAX_LENGTH));
        }
        return stripped;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Task task && id.equals(task.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
