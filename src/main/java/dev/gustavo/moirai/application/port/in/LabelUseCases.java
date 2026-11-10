package dev.gustavo.moirai.application.port.in;

import dev.gustavo.moirai.domain.Label;
import dev.gustavo.moirai.domain.ProjectId;
import dev.gustavo.moirai.domain.UserId;
import java.util.List;

/** Portas de entrada das etiquetas de um projeto. */
public final class LabelUseCases {

    private LabelUseCases() {
    }

    public interface CreateLabel {
        Label create(ProjectId projectId, String name, String color, UserId actor);
    }

    public interface ListLabels {
        /** Etiquetas do projeto, em ordem de nome. Só membros podem ver. */
        List<Label> list(ProjectId projectId, UserId actor);
    }
}
