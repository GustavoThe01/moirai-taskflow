package dev.gustavo.moirai.application.port.out;

import dev.gustavo.moirai.domain.Project;
import dev.gustavo.moirai.domain.ProjectId;
import java.util.Optional;

/** Porta de saída: como a aplicação guarda e recupera projetos, sem saber qual é o banco. */
public interface ProjectRepository {

    void save(Project project);

    Optional<Project> findById(ProjectId id);
}
