package dev.gustavo.moirai.domain;

/** A tarefa pedida não existe. */
public final class TaskNotFoundException extends DomainException {

    private static final long serialVersionUID = 1L;

    public TaskNotFoundException(TaskId id) {
        super("Tarefa %s não encontrada.".formatted(id));
    }
}
