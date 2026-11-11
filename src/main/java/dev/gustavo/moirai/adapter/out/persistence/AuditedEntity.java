package dev.gustavo.moirai.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Colunas de última alteração, preenchidas pelo Spring Data a cada INSERT e UPDATE.
 *
 * <p>Os adapters montam uma entidade nova a cada {@code save}; o JPA faz um {@code merge} e o listener de auditoria
 * roda sobre a entidade gerenciada. Por isso as colunas de criação, nas entidades que as têm, são
 * {@code updatable = false}: uma entidade montada do zero não pode apagar quem criou a linha.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
abstract class AuditedEntity {

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    Instant getUpdatedAt() {
        return updatedAt;
    }

    String getUpdatedBy() {
        return updatedBy;
    }
}
