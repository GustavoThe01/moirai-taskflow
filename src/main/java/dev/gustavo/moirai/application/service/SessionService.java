package dev.gustavo.moirai.application.service;

import dev.gustavo.moirai.application.port.in.SessionUseCases.EndSession;
import dev.gustavo.moirai.application.port.in.SessionUseCases.ListUsers;
import dev.gustavo.moirai.application.port.in.SessionUseCases.RefreshSession;
import dev.gustavo.moirai.application.port.in.SessionUseCases.Session;
import dev.gustavo.moirai.application.port.in.SessionUseCases.StartSession;
import dev.gustavo.moirai.application.port.in.UserUseCases.AuthenticateUser;
import dev.gustavo.moirai.application.port.out.SessionPorts.AccessToken;
import dev.gustavo.moirai.application.port.out.SessionPorts.AccessTokenIssuer;
import dev.gustavo.moirai.application.port.out.SessionPorts.RefreshTokenRepository;
import dev.gustavo.moirai.application.port.out.SessionPorts.StoredRefreshToken;
import dev.gustavo.moirai.application.port.out.UserRepository;
import dev.gustavo.moirai.domain.InvalidRefreshTokenException;
import dev.gustavo.moirai.domain.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class SessionService implements StartSession, RefreshSession, EndSession, ListUsers {

    private static final Logger LOG = LoggerFactory.getLogger(SessionService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthenticateUser authenticateUser;
    private final UserRepository users;
    private final AccessTokenIssuer accessTokens;
    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;
    private final Duration refreshTokenLifetime;

    SessionService(AuthenticateUser authenticateUser, UserRepository users, AccessTokenIssuer accessTokens,
                   RefreshTokenRepository refreshTokens, Clock clock,
                   @Value("${moirai.security.refresh-token-lifetime:P7D}") Duration refreshTokenLifetime) {
        this.authenticateUser = authenticateUser;
        this.users = users;
        this.accessTokens = accessTokens;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
        this.refreshTokenLifetime = refreshTokenLifetime;
    }

    @Override
    @Transactional
    public Session login(String email, String password) {
        User user = authenticateUser.authenticate(email, password);
        return open(user, UUID.randomUUID());
    }

    // noRollbackFor: na reutilização, a revogação da família precisa ser gravada mesmo com a exceção.
    @Override
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public Session refresh(String refreshToken) {
        Instant now = clock.instant();
        StoredRefreshToken stored = find(refreshToken);

        if (stored.isRevoked()) {
            // Um token já trocado voltou: ou o cliente tem um bug, ou alguém copiou o token. Na dúvida,
            // derrubo a sessão inteira; o dono legítimo só precisa fazer login de novo.
            int revoked = refreshTokens.revokeFamily(stored.familyId(), now);
            LOG.warn("Refresh token reutilizado; sessão {} encerrada ({} token(s) revogado(s))", stored.familyId(), revoked);
            throw new InvalidRefreshTokenException();
        }
        if (stored.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        User user = users.findById(stored.userId()).orElseThrow(InvalidRefreshTokenException::new);
        refreshTokens.revoke(stored.id(), now);
        return open(user, stored.familyId());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        // Logout com token desconhecido ou já revogado não é erro: o resultado pedido (sessão encerrada) já vale.
        try {
            StoredRefreshToken stored = find(refreshToken);
            refreshTokens.revokeFamily(stored.familyId(), clock.instant());
        } catch (InvalidRefreshTokenException ignored) {
            // nada a fazer
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> listAll() {
        return users.findAll();
    }

    private Session open(User user, UUID familyId) {
        Instant now = clock.instant();
        AccessToken access = accessTokens.issue(user);
        String refreshToken = newRefreshToken();
        Instant refreshExpiresAt = now.plus(refreshTokenLifetime);
        refreshTokens.save(new StoredRefreshToken(UUID.randomUUID(), user.id(), hash(refreshToken), familyId, refreshExpiresAt, null));
        return new Session(user, access.value(), access.expiresAt(), refreshToken, refreshExpiresAt);
    }

    private StoredRefreshToken find(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        return refreshTokens.findByHash(hash(refreshToken.strip())).orElseThrow(InvalidRefreshTokenException::new);
    }

    /** 32 bytes aleatórios em Base64 URL: impossível de adivinhar e seguro para JSON e cabeçalhos. */
    private static String newRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256, e não BCrypt: o token já tem 256 bits aleatórios, então não há senha fraca para proteger contra força
     * bruta, e o hash precisa ser determinístico para a busca pelo índice.
     */
    static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", exception);
        }
    }
}
