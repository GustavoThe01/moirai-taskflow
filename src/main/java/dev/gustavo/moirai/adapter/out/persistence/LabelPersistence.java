package dev.gustavo.moirai.adapter.out.persistence;

import dev.gustavo.moirai.application.port.out.LabelRepository;
import dev.gustavo.moirai.domain.Label;
import dev.gustavo.moirai.domain.LabelId;
import dev.gustavo.moirai.domain.ProjectId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Tabela {@code label}. */
@Entity
@Table(name = "label")
class LabelJpaEntity {

    @Id
    private String id;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String color;

    protected LabelJpaEntity() {
        // Exigido pelo JPA
    }

    LabelJpaEntity(Label label) {
        this.id = label.id().toString();
        this.projectId = label.projectId().toString();
        this.name = label.name();
        this.color = label.color();
    }

    Label toDomain() {
        return Label.restore(new LabelId(UUID.fromString(id)), new ProjectId(UUID.fromString(projectId)), name, color);
    }
}

interface LabelJpaRepository extends JpaRepository<LabelJpaEntity, String> {

    List<LabelJpaEntity> findByProjectIdOrderByNameAsc(String projectId);
}

@Repository
class LabelPersistenceAdapter implements LabelRepository {

    private final LabelJpaRepository labels;

    LabelPersistenceAdapter(LabelJpaRepository labels) {
        this.labels = labels;
    }

    @Override
    @Transactional
    public void save(Label label) {
        labels.save(new LabelJpaEntity(label));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Label> findByProject(ProjectId projectId) {
        return labels.findByProjectIdOrderByNameAsc(projectId.toString()).stream().map(LabelJpaEntity::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Label> findAllById(Collection<LabelId> ids) {
        return labels.findAllById(ids.stream().map(LabelId::toString).toList()).stream()
                .map(LabelJpaEntity::toDomain)
                .toList();
    }
}
