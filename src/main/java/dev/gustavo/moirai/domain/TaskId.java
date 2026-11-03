package dev.gustavo.moirai.domain;

import java.util.Objects;
import java.util.UUID;

/** Identificador de tarefa. Tipado para que um TaskId não seja trocado por outro id por engano. */
public record TaskId(UUID value) {

    public TaskId {
        Objects.requireNonNull(value, "value");
    }

    public static TaskId newId() {
        return new TaskId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
