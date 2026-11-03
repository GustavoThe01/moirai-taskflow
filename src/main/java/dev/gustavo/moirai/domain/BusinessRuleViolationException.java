package dev.gustavo.moirai.domain;

/** Regra de negócio que impede a operação (ex.: iniciar uma tarefa sem responsável). */
public final class BusinessRuleViolationException extends DomainException {

    private static final long serialVersionUID = 1L;

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
