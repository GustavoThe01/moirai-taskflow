package dev.gustavo.moirai.application.port.out;

import dev.gustavo.moirai.domain.Project;
import dev.gustavo.moirai.domain.ProjectId;
import dev.gustavo.moirai.domain.UserId;
import java.util.List;
import java.util.Optional;

/** Porta de saída: como a aplicação guarda e recupera projetos, sem saber qual é o banco. */
public interface ProjectRepository {

    void save(Project project);

    Optional<Project> findById(ProjectId id);

    /** Projetos dos quais o usuário é membro, em ordem de nome. */
    List<Project> findByMember(UserId member);

    void delete(ProjectId id);
}
