package dev.gustavo.moirai.domain;

/** Tentativa de mover a tarefa para um status que não é permitido a partir do status atual. */
public final class InvalidTaskTransitionException extends DomainException {

    private static final long serialVersionUID = 1L;

    public InvalidTaskTransitionException(TaskStatus from, TaskStatus to) {
        super("Uma tarefa em %s não pode ir para %s.".formatted(from, to));
    }
}
