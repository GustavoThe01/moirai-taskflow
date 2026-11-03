package dev.gustavo.moirai.domain;

import java.util.Objects;
import java.util.UUID;

/** Identificador de projeto. Tipado para que um ProjectId não seja trocado por outro id por engano. */
public record ProjectId(UUID value) {

    public ProjectId {
        Objects.requireNonNull(value, "value");
    }

    public static ProjectId newId() {
        return new ProjectId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
