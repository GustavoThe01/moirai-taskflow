package dev.gustavo.moirai.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Status de uma tarefa e as transições permitidas entre eles.
 *
 * <pre>
 * TODO ──► IN_PROGRESS ──► REVIEW ──► DONE
 *   ▲          │  ▲           │
 *   └──────────┘  └───────────┘
 * </pre>
 *
 * <p>DONE é final: uma tarefa concluída não volta. Se algo ficou faltando, abre-se outra tarefa.
 */
public enum TaskStatus {
    TODO,
    IN_PROGRESS,
    REVIEW,
    DONE;

    public Set<TaskStatus> allowedTransitions() {
        return switch (this) {
            case TODO -> EnumSet.of(IN_PROGRESS);
            case IN_PROGRESS -> EnumSet.of(TODO, REVIEW);
            case REVIEW -> EnumSet.of(IN_PROGRESS, DONE);
            case DONE -> EnumSet.noneOf(TaskStatus.class);
        };
    }

    public boolean canTransitionTo(TaskStatus target) {
        return allowedTransitions().contains(target);
    }

    public boolean isFinal() {
        return this == DONE;
    }
}
