package dev.gustavo.moirai.adapter.out.persistence;

import dev.gustavo.moirai.application.port.out.SessionPorts.RefreshTokenRepository;
import dev.gustavo.moirai.application.port.out.SessionPorts.StoredRefreshToken;
import dev.gustavo.moirai.domain.UserId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Tabela {@code refresh_token}. */
@Entity
@Table(name = "refresh_token")
class RefreshTokenJpaEntity {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "family_id", nullable = false)
    private String familyId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected RefreshTokenJpaEntity() {
        // Exigido pelo JPA
    }

    RefreshTokenJpaEntity(StoredRefreshToken token) {
        this.id = token.id().toString();
        this.userId = token.userId().toString();
        this.tokenHash = token.tokenHash();
        this.familyId = token.familyId().toString();
        this.expiresAt = token.expiresAt();
        this.revokedAt = token.revokedAt();
    }

    StoredRefreshToken toDomain() {
        return new StoredRefreshToken(UUID.fromString(id), new UserId(UUID.fromString(userId)), tokenHash,
                UUID.fromString(familyId), expiresAt, revokedAt);
    }
}

interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, String> {

    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshTokenJpaEntity t set t.revokedAt = :at where t.id = :id and t.revokedAt is null")
    int revoke(@Param("id") String id, @Param("at") Instant at);

    @Modifying
    @Query("update RefreshTokenJpaEntity t set t.revokedAt = :at where t.familyId = :familyId and t.revokedAt is null")
    int revokeFamily(@Param("familyId") String familyId, @Param("at") Instant at);
}

@Repository
class RefreshTokenPersistenceAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository tokens;

    RefreshTokenPersistenceAdapter(RefreshTokenJpaRepository tokens) {
        this.tokens = tokens;
    }

    @Override
    @Transactional
    public void save(StoredRefreshToken token) {
        tokens.save(new RefreshTokenJpaEntity(token));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredRefreshToken> findByHash(String tokenHash) {
        return tokens.findByTokenHash(tokenHash).map(RefreshTokenJpaEntity::toDomain);
    }

    @Override
    @Transactional
    public void revoke(UUID id, Instant at) {
        tokens.revoke(id.toString(), at);
    }

    @Override
    @Transactional
    public int revokeFamily(UUID familyId, Instant at) {
        return tokens.revokeFamily(familyId.toString(), at);
    }
}
