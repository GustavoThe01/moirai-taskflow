package dev.gustavo.moirai.config;

import java.time.Clock;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Auditoria do JPA: quem e quando criou ou alterou cada linha.
 *
 * <p>É um detalhe de persistência, e por isso fica fora do domínio. O usuário é o dono do access token da
 * requisição em andamento (claim {@code sub}). Fora de uma requisição autenticada (um teste, um job), não há usuário,
 * e as colunas {@code *_by} ficam vazias.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "currentUser", dateTimeProviderRef = "auditClock")
class AuditingConfig {

    @Bean
    AuditorAware<String> currentUser() {
        return () -> Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .filter(JwtAuthenticationToken.class::isInstance)
                .map(authentication -> ((JwtAuthenticationToken) authentication).getToken().getSubject());
    }

    /** As datas de auditoria vêm do mesmo relógio da aplicação, que os testes conseguem controlar. */
    @Bean
    DateTimeProvider auditClock(Clock clock) {
        return () -> Optional.of(clock.instant());
    }
}
