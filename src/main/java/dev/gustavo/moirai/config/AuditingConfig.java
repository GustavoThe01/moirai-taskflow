package dev.gustavo.moirai.config;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Auditoria do JPA: quem e quando criou ou alterou cada linha.
 *
 * <p>É um detalhe de persistência, e por isso fica fora do domínio. O usuário vem do cabeçalho {@code X-User-Id}
 * da requisição em andamento até o Dia 12, quando passa a vir do JWT. Fora de uma requisição (um teste, um job),
 * não há usuário, e as colunas {@code *_by} ficam vazias.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "currentUser", dateTimeProviderRef = "auditClock")
class AuditingConfig {

    static final String USER_HEADER = "X-User-Id";

    @Bean
    AuditorAware<String> currentUser() {
        return () -> Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(attributes -> ((ServletRequestAttributes) attributes).getRequest())
                .map(AuditingConfig::userFrom);
    }

    /** As datas de auditoria vêm do mesmo relógio da aplicação, que os testes conseguem controlar. */
    @Bean
    DateTimeProvider auditClock(Clock clock) {
        return () -> Optional.of(clock.instant());
    }

    private static String userFrom(HttpServletRequest request) {
        String header = request.getHeader(USER_HEADER);
        if (header == null) {
            return null;
        }
        try {
            return UUID.fromString(header.strip()).toString();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
