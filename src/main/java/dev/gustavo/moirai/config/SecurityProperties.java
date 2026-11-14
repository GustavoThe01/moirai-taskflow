package dev.gustavo.moirai.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * As configurações de segurança ({@code moirai.security.*}), conferidas na partida. Um valor inválido derruba a
 * aplicação ao subir, com a mensagem do campo, em vez de virar um erro estranho na primeira requisição.
 *
 * @param jwtSecret chave HMAC em Base64. Vem da variável de ambiente {@code MOIRAI_JWT_SECRET}, nunca do repositório.
 * @param requireJwtSecret no profile {@code prod}, {@code true}: sem a chave, a aplicação não sobe. Nos outros
 *     profiles, sem a chave, uma chave temporária é gerada a cada execução.
 */
@Validated
@ConfigurationProperties("moirai.security")
public record SecurityProperties(
        String jwtSecret,
        boolean requireJwtSecret,
        @NotBlank String issuer,
        @NotNull Duration accessTokenLifetime,
        @NotNull Duration refreshTokenLifetime,
        List<String> adminEmails) {

    static final int MIN_KEY_BYTES = 32;

    public SecurityProperties {
        jwtSecret = jwtSecret == null ? "" : jwtSecret.strip();
        adminEmails = adminEmails == null ? List.of() : List.copyOf(adminEmails);
    }

    @AssertTrue(message = "moirai.security.jwt-secret (MOIRAI_JWT_SECRET) é obrigatória neste profile")
    public boolean isJwtSecretPresentWhenRequired() {
        return !requireJwtSecret || !jwtSecret.isEmpty();
    }

    @AssertTrue(message = "moirai.security.jwt-secret precisa ser Base64 com pelo menos 32 bytes")
    public boolean isJwtSecretStrongWhenPresent() {
        if (jwtSecret.isEmpty()) {
            return true;
        }
        try {
            return Base64.getDecoder().decode(jwtSecret).length >= MIN_KEY_BYTES;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @AssertTrue(message = "o access token precisa durar entre 1 minuto e 1 hora, e o refresh token mais que ele")
    public boolean isLifetimeSane() {
        return accessTokenLifetime == null || refreshTokenLifetime == null
                || accessTokenLifetime.compareTo(Duration.ofMinutes(1)) >= 0
                        && accessTokenLifetime.compareTo(Duration.ofHours(1)) <= 0
                        && refreshTokenLifetime.compareTo(accessTokenLifetime) > 0;
    }
}
