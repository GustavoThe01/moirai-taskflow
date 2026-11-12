package dev.gustavo.moirai.application.port.out;

import dev.gustavo.moirai.domain.User;
import java.util.Optional;

/** Porta de saída: como a aplicação guarda e recupera usuários. */
public interface UserRepository {

    void save(User user);

    /** Busca pelo e-mail já normalizado (minúsculo, sem espaços). */
    Optional<User> findByEmail(String normalizedEmail);

    boolean existsByEmail(String normalizedEmail);
}
