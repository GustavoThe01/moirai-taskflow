package dev.gustavo.moirai.adapter.out.persistence;

import dev.gustavo.moirai.application.port.out.CommentRepository;
import dev.gustavo.moirai.domain.Comment;
import dev.gustavo.moirai.domain.CommentId;
import dev.gustavo.moirai.domain.TaskId;
import dev.gustavo.moirai.domain.UserId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Tabela {@code comment}. */
@Entity
@Table(name = "comment")
class CommentJpaEntity {

    @Id
    private String id;

    @Column(name = "task_id", nullable = false)
    private String taskId;

    @Column(name = "author_id", nullable = false)
    private String authorId;

    @Column(nullable = false)
    private String text;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    protected CommentJpaEntity() {
        // Exigido pelo JPA
    }

    CommentJpaEntity(Comment comment) {
        this.id = comment.id().toString();
        this.taskId = comment.taskId().toString();
        this.authorId = comment.author().toString();
        this.text = comment.text();
        this.createdAt = comment.createdAt().toString();
    }

    Comment toDomain() {
        return Comment.restore(new CommentId(UUID.fromString(id)), new TaskId(UUID.fromString(taskId)),
                new UserId(UUID.fromString(authorId)), text, Instant.parse(createdAt));
    }
}

interface CommentJpaRepository extends JpaRepository<CommentJpaEntity, String> {

    List<CommentJpaEntity> findByTaskIdOrderByCreatedAtAscIdAsc(String taskId);
}

@Repository
class CommentPersistenceAdapter implements CommentRepository {

    private final CommentJpaRepository comments;

    CommentPersistenceAdapter(CommentJpaRepository comments) {
        this.comments = comments;
    }

    @Override
    @Transactional
    public void save(Comment comment) {
        comments.save(new CommentJpaEntity(comment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comment> findByTask(TaskId taskId) {
        return comments.findByTaskIdOrderByCreatedAtAscIdAsc(taskId.toString()).stream()
                .map(CommentJpaEntity::toDomain)
                .toList();
    }
}
