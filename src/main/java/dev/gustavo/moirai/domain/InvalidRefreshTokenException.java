package dev.gustavo.moirai.domain;

/** Refresh token desconhecido, vencido ou já usado. A mensagem não diz qual dos três, de propósito. */
public final class InvalidRefreshTokenException extends DomainException {

    private static final long serialVersionUID = 1L;

    public InvalidRefreshTokenException() {
        super("Refresh token inválido ou vencido. Faça login de novo.");
    }
}
