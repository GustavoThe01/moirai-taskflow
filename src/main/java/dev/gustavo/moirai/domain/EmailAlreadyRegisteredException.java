package dev.gustavo.moirai.domain;

/** Já existe uma conta com este e-mail. */
public final class EmailAlreadyRegisteredException extends DomainException {

    private static final long serialVersionUID = 1L;

    public EmailAlreadyRegisteredException(String email) {
        super("Já existe uma conta com o e-mail " + email + ".");
    }
}
