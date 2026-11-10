package dev.gustavo.moirai.domain;

import java.util.Objects;
import java.util.UUID;

/** Identificador de comentário. */
public record CommentId(UUID value) {

    public CommentId {
        Objects.requireNonNull(value, "value");
    }

    public static CommentId newId() {
        return new CommentId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
