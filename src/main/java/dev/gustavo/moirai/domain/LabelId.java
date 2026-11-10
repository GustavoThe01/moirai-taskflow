package dev.gustavo.moirai.domain;

import java.util.Objects;
import java.util.UUID;

/** Identificador de etiqueta. */
public record LabelId(UUID value) {

    public LabelId {
        Objects.requireNonNull(value, "value");
    }

    public static LabelId newId() {
        return new LabelId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
