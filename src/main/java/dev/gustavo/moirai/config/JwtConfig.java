package dev.gustavo.moirai.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
 * <p>A chave HMAC vem de {@code moirai.security.jwt-secret}, preenchida pela variável de ambiente
 * {@code MOIRAI_JWT_SECRET} e nunca pelo repositório. Nos profiles {@code dev} e {@code test}, sem ela, gero uma chave
 * aleatória a cada execução e aviso no log. No profile {@code prod}, a aplicação não sobe sem ela.
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
class JwtConfig {

    private static final Logger LOG = LoggerFactory.getLogger(JwtConfig.class);

    /** A chave já chega validada pelo {@link SecurityProperties}: vazia (fora de prod) ou Base64 com 32 bytes ou mais. */
    @Bean
    SecretKey jwtSigningKey(SecurityProperties security) {
        byte[] bytes;
        if (security.jwtSecret().isEmpty()) {
            bytes = new byte[SecurityProperties.MIN_KEY_BYTES];
            new SecureRandom().nextBytes(bytes);
            LOG.warn("MOIRAI_JWT_SECRET não configurada: usando uma chave temporária. Tokens não sobrevivem a um restart.");
        } else {
            bytes = Base64.getDecoder().decode(security.jwtSecret());
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    /** Confere assinatura (só HS256), validade com a tolerância padrão de 60 s e o emissor. */
    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey, SecurityProperties security) {
        String issuer = security.issuer();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer)));
        return decoder;
    }
}
