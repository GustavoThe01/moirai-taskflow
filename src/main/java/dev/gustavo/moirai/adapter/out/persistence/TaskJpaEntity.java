package dev.gustavo.moirai.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tabela {@code task}. Datas e enums ficam como texto, e a conversão acontece em {@link PersistenceMapper}. */
@Entity
@Table(name = "task")
class TaskJpaEntity {

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

    protected TaskJpaEntity() {
        // Exigido pelo JPA
    }

    TaskJpaEntity(String id, String projectId, String createdBy, String createdAt, String title,
                  String description, String priority, String dueDate, String status, String assigneeId) {
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
}
