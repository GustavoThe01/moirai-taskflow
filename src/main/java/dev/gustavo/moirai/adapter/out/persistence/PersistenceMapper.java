package dev.gustavo.moirai.adapter.out.persistence;

import dev.gustavo.moirai.domain.Project;
import dev.gustavo.moirai.domain.ProjectId;
import dev.gustavo.moirai.domain.Task;
import dev.gustavo.moirai.domain.TaskId;
import dev.gustavo.moirai.domain.TaskPriority;
import dev.gustavo.moirai.domain.TaskStatus;
import dev.gustavo.moirai.domain.UserId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Collectors;

/** Converte entre o domínio e as entidades JPA. Decisão de escrevê-lo à mão no ADR 0003. */
final class PersistenceMapper {

    private PersistenceMapper() {
    }

    static ProjectJpaEntity toEntity(Project project) {
        return new ProjectJpaEntity(
                project.id().toString(),
                project.name(),
                project.ownerId().toString(),
                project.members().stream().map(UserId::toString).collect(Collectors.toSet()));
    }

    static Project toDomain(ProjectJpaEntity entity) {
        return Project.restore(
                new ProjectId(UUID.fromString(entity.getId())),
                entity.getName(),
                userId(entity.getOwnerId()),
                entity.getMembers().stream().map(PersistenceMapper::userId).toList());
    }

    static TaskJpaEntity toEntity(Task task) {
        return new TaskJpaEntity(
                task.id().toString(),
                task.projectId().toString(),
                task.createdBy().toString(),
                task.createdAt().toString(),
                task.title(),
                task.description(),
                task.priority().name(),
                task.dueDate().map(LocalDate::toString).orElse(null),
                task.status().name(),
                task.assigneeId().map(UserId::toString).orElse(null));
    }

    static Task toDomain(TaskJpaEntity entity) {
        return Task.restore(
                new TaskId(UUID.fromString(entity.getId())),
                new ProjectId(UUID.fromString(entity.getProjectId())),
                userId(entity.getCreatedBy()),
                Instant.parse(entity.getCreatedAt()),
                entity.getTitle(),
                entity.getDescription(),
                TaskPriority.valueOf(entity.getPriority()),
                entity.getDueDate() == null ? null : LocalDate.parse(entity.getDueDate()),
                TaskStatus.valueOf(entity.getStatus()),
                entity.getAssigneeId() == null ? null : userId(entity.getAssigneeId()));
    }

    private static UserId userId(String value) {
        return new UserId(UUID.fromString(value));
    }
}
