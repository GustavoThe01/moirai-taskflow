package dev.gustavo.moirai.adapter.out.persistence;

import dev.gustavo.moirai.application.port.out.ProjectRepository;
import dev.gustavo.moirai.domain.Project;
import dev.gustavo.moirai.domain.ProjectId;
import dev.gustavo.moirai.domain.UserId;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class ProjectPersistenceAdapter implements ProjectRepository {

    private final ProjectJpaRepository projects;
    private final Clock clock;

    ProjectPersistenceAdapter(ProjectJpaRepository projects, Clock clock) {
        this.projects = projects;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void save(Project project) {
        projects.save(PersistenceMapper.toEntity(project));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Project> findById(ProjectId id) {
        return projects.findById(id.toString()).map(PersistenceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findByMember(UserId member) {
        return projects.findByMember(member.toString()).stream().map(PersistenceMapper::toDomain).toList();
    }

    /** Exclusão lógica: a linha fica, com {@code deleted_at}, e some de todas as consultas da aplicação. */
    @Override
    @Transactional
    public void delete(ProjectId id) {
        projects.softDelete(id.toString(), clock.instant());
    }
}
