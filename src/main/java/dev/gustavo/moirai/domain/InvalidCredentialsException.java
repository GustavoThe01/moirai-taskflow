package dev.gustavo.moirai.domain;

/**
 * E-mail ou senha não conferem. A mensagem é a mesma nos dois casos, de propósito: dizer qual dos dois errou
 * revelaria quais e-mails têm conta.
 */
public final class InvalidCredentialsException extends DomainException {

    private static final long serialVersionUID = 1L;

    public InvalidCredentialsException() {
        super("E-mail ou senha inválidos.");
    }
}
