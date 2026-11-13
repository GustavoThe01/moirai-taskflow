package dev.gustavo.moirai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.gustavo.moirai.application.port.in.SessionUseCases.Session;
import dev.gustavo.moirai.application.port.in.UserUseCases.AuthenticateUser;
import dev.gustavo.moirai.application.port.out.SessionPorts.AccessToken;
import dev.gustavo.moirai.application.port.out.SessionPorts.AccessTokenIssuer;
import dev.gustavo.moirai.application.port.out.SessionPorts.RefreshTokenRepository;
import dev.gustavo.moirai.application.port.out.SessionPorts.StoredRefreshToken;
import dev.gustavo.moirai.application.port.out.UserRepository;
import dev.gustavo.moirai.domain.InvalidRefreshTokenException;
import dev.gustavo.moirai.domain.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Rotação e revogação dos refresh tokens, sem Spring, sem banco e sem JWT. */
@DisplayName("SessionService")
class SessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-13T12:00:00Z");

    private final AuthenticateUser authenticate = mock(AuthenticateUser.class);
    private final UserRepository users = mock(UserRepository.class);
    private final AccessTokenIssuer accessTokens = mock(AccessTokenIssuer.class);
    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    private final SessionService service = new SessionService(authenticate, users, accessTokens, refreshTokens,
            Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofDays(7));
    private final User ana = User.register("ana@exemplo.com", "Ana", "hash", NOW);

    SessionServiceTest() {
        when(accessTokens.issue(any())).thenReturn(new AccessToken("jwt", NOW.plusSeconds(900)));
        when(users.findById(ana.id())).thenReturn(Optional.of(ana));
    }

    private StoredRefreshToken stored(String token, UUID family, Instant expiresAt, Instant revokedAt) {
        var stored = new StoredRefreshToken(UUID.randomUUID(), ana.id(), SessionService.hash(token), family, expiresAt, revokedAt);
        when(refreshTokens.findByHash(SessionService.hash(token))).thenReturn(Optional.of(stored));
        return stored;
    }

    @Test
    @DisplayName("login grava só o hash do refresh token, numa família nova, com 7 dias")
    void loginStoresHash() {
        when(authenticate.authenticate("ana@exemplo.com", "senha")).thenReturn(ana);

        Session session = service.login("ana@exemplo.com", "senha");

        var saved = ArgumentCaptor.forClass(StoredRefreshToken.class);
        verify(refreshTokens).save(saved.capture());
        assertThat(saved.getValue().tokenHash()).isEqualTo(SessionService.hash(session.refreshToken())).isNotEqualTo(session.refreshToken());
        assertThat(saved.getValue().expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        assertThat(session.refreshToken()).hasSize(43); // 32 bytes em Base64 URL sem padding
        assertThat(session.toString()).doesNotContain(session.refreshToken());
    }

    @Test
    @DisplayName("renovar revoga o token usado e mantém a família")
    void refreshRotates() {
        UUID family = UUID.randomUUID();
        StoredRefreshToken old = stored("antigo", family, NOW.plusSeconds(3600), null);

        service.refresh("antigo");

        verify(refreshTokens).revoke(old.id(), NOW);
        var saved = ArgumentCaptor.forClass(StoredRefreshToken.class);
        verify(refreshTokens).save(saved.capture());
        assertThat(saved.getValue().familyId()).isEqualTo(family);
    }

    @Test
    @DisplayName("token já revogado que volta derruba a família inteira")
    void reuseRevokesFamily() {
        UUID family = UUID.randomUUID();
        stored("copiado", family, NOW.plusSeconds(3600), NOW.minusSeconds(60));

        assertThatThrownBy(() -> service.refresh("copiado")).isInstanceOf(InvalidRefreshTokenException.class);
        verify(refreshTokens).revokeFamily(family, NOW);
        verify(refreshTokens, never()).save(any());
    }

    @Test
    @DisplayName("token vencido ou desconhecido não abre sessão")
    void expiredOrUnknown() {
        stored("vencido", UUID.randomUUID(), NOW, null);
        when(refreshTokens.findByHash(SessionService.hash("inventado"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("vencido")).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> service.refresh("inventado")).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> service.refresh(" ")).isInstanceOf(InvalidRefreshTokenException.class);
        verify(accessTokens, never()).issue(any());
    }

    @Test
    @DisplayName("logout revoga a família; token desconhecido não é erro")
    void logout() {
        UUID family = UUID.randomUUID();
        stored("ativo", family, NOW.plusSeconds(3600), null);
        when(refreshTokens.findByHash(SessionService.hash("ninguem"))).thenReturn(Optional.empty());

        service.logout("ativo");
        service.logout("ninguem");

        verify(refreshTokens).revokeFamily(family, NOW);
    }
}
