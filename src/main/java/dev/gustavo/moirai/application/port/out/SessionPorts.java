package dev.gustavo.moirai.application.port.out;

import dev.gustavo.moirai.domain.User;
import dev.gustavo.moirai.domain.UserId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Portas de saída da sessão. */
public final class SessionPorts {

    private SessionPorts() {
    }

    public record AccessToken(String value, Instant expiresAt) {
    }

    /** Emite o access token. A aplicação não sabe que ele é um JWT assinado com HMAC. */
    public interface AccessTokenIssuer {
        AccessToken issue(User user);
    }

    /**
     * Refresh token como fica guardado: só o hash SHA-256, nunca o texto. Quem tiver acesso ao banco não consegue
     * usar os tokens dali. A família liga todos os tokens nascidos do mesmo login.
     */
    public record StoredRefreshToken(UUID id, UserId userId, String tokenHash, UUID familyId, Instant expiresAt, Instant revokedAt) {

        public boolean isRevoked() {
            return revokedAt != null;
        }

        public boolean isExpired(Instant now) {
            return !now.isBefore(expiresAt);
        }
    }

    public interface RefreshTokenRepository {

        void save(StoredRefreshToken token);

        Optional<StoredRefreshToken> findByHash(String tokenHash);

        /** Revoga um token. Não muda a data de quem já estava revogado. */
        void revoke(UUID id, Instant at);

        /** Revoga todos os tokens ainda válidos da família. */
        int revokeFamily(UUID familyId, Instant at);
    }
}
