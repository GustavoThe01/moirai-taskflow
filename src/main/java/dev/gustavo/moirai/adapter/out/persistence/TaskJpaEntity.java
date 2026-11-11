package dev.gustavo.moirai.adapter.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;
import java.util.HashSet;
import java.util.Set;

/** Tabela {@code task}. Datas e enums ficam como texto, e a conversão acontece em {@link PersistenceMapper}. */
@Entity
@Table(name = "task")
class TaskJpaEntity extends AuditedEntity {

    @Id
    private String id;

    @Column(name = "project_id", nullable = false)
    private String projectId;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String priority;

    @Column(name = "due_date")
    private String dueDate;

    @Column(nullable = false)
    private String status;

    @Column(name = "assignee_id")
    private String assigneeId;

    /**
     * Lado da tarefa na relação N:N com etiquetas: os ids ficam em {@code task_label}.
     *
     * <p>{@code @BatchSize} corrige o N+1 da listagem: ao tocar nas etiquetas da primeira tarefa da página, o Hibernate
     * carrega as das outras (até 50) na mesma consulta, com {@code task_id IN (...)}. Um {@code fetch join} não serve
     * aqui: com paginação e coleção, o Hibernate traria tudo e paginaria em memória.
     */
    @ElementCollection
    @BatchSize(size = 50)
    @CollectionTable(name = "task_label", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "label_id")
    private Set<String> labels = new HashSet<>();

    protected TaskJpaEntity() {
        // Exigido pelo JPA
    }

    TaskJpaEntity(String id, String projectId, String createdBy, String createdAt, String title,
                  String description, String priority, String dueDate, String status, String assigneeId,
                  Set<String> labels) {
        this.id = id;
        this.projectId = projectId;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
        this.status = status;
        this.assigneeId = assigneeId;
        this.labels = new HashSet<>(labels);
    }

    String getId() {
        return id;
    }

    String getProjectId() {
        return projectId;
    }

    String getCreatedBy() {
        return createdBy;
    }

    String getCreatedAt() {
        return createdAt;
    }

    String getTitle() {
        return title;
    }

    String getDescription() {
        return description;
    }

    String getPriority() {
        return priority;
    }

    String getDueDate() {
        return dueDate;
    }

    String getStatus() {
        return status;
    }

    String getAssigneeId() {
        return assigneeId;
    }

    Set<String> getLabels() {
        return labels;
    }
}
