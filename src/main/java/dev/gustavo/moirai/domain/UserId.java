package dev.gustavo.moirai.domain;

import java.util.Objects;
import java.util.UUID;

/** Identificador de usuário. Tipado para que um UserId não seja trocado por outro id por engano. */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "value");
    }

    public static UserId newId() {
        return new UserId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
