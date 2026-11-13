package dev.gustavo.moirai.adapter.out.security;

import dev.gustavo.moirai.application.port.out.SessionPorts.AccessToken;
import dev.gustavo.moirai.application.port.out.SessionPorts.AccessTokenIssuer;
import dev.gustavo.moirai.domain.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Access token como JWT assinado com HMAC-SHA256. O {@code sub} é o id do usuário e {@code roles} traz o papel.
 *
 * <p>Ele vive pouco (15 minutos por padrão): é conferido só pela assinatura, sem consultar o banco, então um logout
 * não o invalida. Quem precisa de uma sessão longa usa o refresh token, que fica no banco e pode ser revogado.
 */
@Component
class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtEncoder encoder;
    private final Clock clock;
    private final String issuer;
    private final Duration lifetime;

    JwtAccessTokenIssuer(JwtEncoder encoder, Clock clock,
                         @Value("${moirai.security.issuer:moirai-taskflow}") String issuer,
                         @Value("${moirai.security.access-token-lifetime:PT15M}") Duration lifetime) {
        this.encoder = encoder;
        this.clock = clock;
        this.issuer = issuer;
        this.lifetime = lifetime;
    }

    @Override
    public AccessToken issue(User user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(lifetime);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.id().toString())
                .issuedAt(now)
                .notBefore(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("email", user.email())
                .claim("roles", List.of(user.role().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return new AccessToken(encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(), expiresAt);
    }
}
