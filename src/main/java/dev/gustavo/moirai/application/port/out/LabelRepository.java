package dev.gustavo.moirai.application.port.out;

import dev.gustavo.moirai.domain.Label;
import dev.gustavo.moirai.domain.LabelId;
import dev.gustavo.moirai.domain.ProjectId;
import java.util.Collection;
import java.util.List;

/** Porta de saída: como a aplicação guarda e recupera etiquetas. */
public interface LabelRepository {

    void save(Label label);

    /** Etiquetas do projeto, em ordem de nome. */
    List<Label> findByProject(ProjectId projectId);

    /** As etiquetas encontradas; ids que não existem simplesmente não voltam. */
    List<Label> findAllById(Collection<LabelId> ids);
}
