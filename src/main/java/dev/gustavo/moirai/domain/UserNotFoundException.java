package dev.gustavo.moirai.domain;

/** Não existe conta com o e-mail informado. */
public final class UserNotFoundException extends DomainException {

    private static final long serialVersionUID = 1L;

    public UserNotFoundException(String email) {
        super("Nenhuma conta com o e-mail %s.".formatted(email));
    }
}
