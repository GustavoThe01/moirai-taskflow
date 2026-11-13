package dev.gustavo.moirai.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Chave, emissor e decodificador do JWT.
 *
 * <p>A chave HMAC vem de {@code moirai.security.jwt-secret} (Base64, pelo menos 32 bytes), definida por variável de
 * ambiente e nunca no repositório. Sem ela, gero uma chave aleatória a cada execução e aviso no log: serve para rodar
 * e testar, mas os tokens deixam de valer quando a aplicação reinicia. No Dia 14, o profile {@code prod} vai exigir
 * a chave.
 */
@Configuration
class JwtConfig {

    private static final Logger LOG = LoggerFactory.getLogger(JwtConfig.class);
    private static final int MIN_KEY_BYTES = 32;

    @Bean
    SecretKey jwtSigningKey(@Value("${moirai.security.jwt-secret:}") String secret) {
        byte[] bytes;
        if (secret.isBlank()) {
            bytes = new byte[MIN_KEY_BYTES];
            new SecureRandom().nextBytes(bytes);
            LOG.warn("moirai.security.jwt-secret não configurada: usando uma chave temporária. Tokens não sobrevivem a um restart.");
        } else {
            bytes = Base64.getDecoder().decode(secret.strip());
            if (bytes.length < MIN_KEY_BYTES) {
                throw new IllegalStateException("moirai.security.jwt-secret precisa ter pelo menos %d bytes.".formatted(MIN_KEY_BYTES));
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    /** Confere assinatura (só HS256), validade com a tolerância padrão de 60 s e o emissor. */
    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey, @Value("${moirai.security.issuer:moirai-taskflow}") String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer)));
        return decoder;
    }
}
