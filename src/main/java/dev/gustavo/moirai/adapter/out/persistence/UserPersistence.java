package dev.gustavo.moirai.adapter.out.persistence;

import dev.gustavo.moirai.application.port.out.UserRepository;
import dev.gustavo.moirai.domain.User;
import dev.gustavo.moirai.domain.UserId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Tabela {@code app_user}: {@code user} é palavra reservada no PostgreSQL, que chega no Dia 15. */
@Entity
@Table(name = "app_user")
class UserJpaEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserJpaEntity() {
        // Exigido pelo JPA
    }

    UserJpaEntity(User user) {
        this.id = user.id().toString();
        this.email = user.email();
        this.name = user.name();
        this.passwordHash = user.passwordHash();
        this.createdAt = user.createdAt();
    }

    User toDomain() {
        return User.restore(new UserId(UUID.fromString(id)), email, name, passwordHash, createdAt);
    }
}

interface UserJpaRepository extends JpaRepository<UserJpaEntity, String> {

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}

@Repository
class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository users;

    UserPersistenceAdapter(UserJpaRepository users) {
        this.users = users;
    }

    @Override
    @Transactional
    public void save(User user) {
        users.save(new UserJpaEntity(user));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String normalizedEmail) {
        return users.findByEmail(normalizedEmail).map(UserJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String normalizedEmail) {
        return users.existsByEmail(normalizedEmail);
    }
}
