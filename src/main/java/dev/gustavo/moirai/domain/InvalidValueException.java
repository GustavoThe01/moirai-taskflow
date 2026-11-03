package dev.gustavo.moirai.domain;

/** Valor que não respeita as regras do seu tipo (título curto demais, prazo no passado...). */
public final class InvalidValueException extends DomainException {

    private static final long serialVersionUID = 1L;

    public InvalidValueException(String message) {
        super(message);
    }
}
