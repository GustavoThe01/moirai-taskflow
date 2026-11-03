package dev.gustavo.moirai.domain;

/**
 * Base das violações de regra de negócio do domínio.
 *
 * <p>No Dia 05, o adapter web vai traduzir cada subclasse para um ProblemDetail com o status HTTP adequado.
 */
public abstract class DomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected DomainException(String message) {
        super(message);
    }
}
