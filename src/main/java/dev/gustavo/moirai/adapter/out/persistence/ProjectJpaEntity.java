package dev.gustavo.moirai.adapter.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/** Tabela {@code project}. Separada do domínio: mudanças no banco não encostam nas regras de negócio. */
@Entity
@Table(name = "project")
class ProjectJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @ElementCollection
    @CollectionTable(name = "project_member", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "user_id")
    private Set<String> members = new HashSet<>();

    protected ProjectJpaEntity() {
        // Exigido pelo JPA
    }

    ProjectJpaEntity(String id, String name, String ownerId, Set<String> members) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.members = new HashSet<>(members);
    }

    String getId() {
        return id;
    }

    String getName() {
        return name;
    }

    String getOwnerId() {
        return ownerId;
    }

    Set<String> getMembers() {
        return members;
    }
}
