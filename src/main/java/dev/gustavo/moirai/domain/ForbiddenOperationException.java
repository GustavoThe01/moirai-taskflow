package dev.gustavo.moirai.domain;

/** O usuário é conhecido, mas o papel dele não permite a operação (ex.: só o dono exclui o projeto). */
public final class ForbiddenOperationException extends DomainException {

    private static final long serialVersionUID = 1L;

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
