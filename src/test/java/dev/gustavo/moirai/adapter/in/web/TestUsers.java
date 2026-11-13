package dev.gustavo.moirai.adapter.in.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import dev.gustavo.moirai.domain.UserId;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Autentica uma requisição do MockMvc como um usuário, sem passar pelo login. O {@code sub} do token é o id
 * informado, como num access token de verdade. A emissão e a validação reais estão testadas no {@code JwtFlowTest}.
 */
public final class TestUsers {

    private TestUsers() {
    }

    public static RequestPostProcessor as(String userId) {
        return jwt().jwt(token -> token.subject(userId).claim("roles", List.of("USER")))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public static RequestPostProcessor as(UserId userId) {
        return as(userId.toString());
    }

    public static RequestPostProcessor as(java.util.UUID userId) {
        return as(userId.toString());
    }

    public static RequestPostProcessor admin(String userId) {
        return jwt().jwt(token -> token.subject(userId).claim("roles", List.of("ADMIN")))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
