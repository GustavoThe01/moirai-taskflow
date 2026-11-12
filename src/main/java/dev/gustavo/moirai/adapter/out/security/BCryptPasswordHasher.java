package dev.gustavo.moirai.adapter.out.security;

import dev.gustavo.moirai.application.port.out.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Hash de senha com BCrypt, do Spring Security.
 *
 * <p>O BCrypt gera um sal novo a cada hash e guarda o sal e o custo dentro do próprio texto ({@code $2a$10$...}).
 * Custo 10 é o padrão do Spring: cada conferência leva dezenas de milissegundos, o que torna inviável testar
 * bilhões de senhas, mas não pesa num login.
 */
@Component
class BCryptPasswordHasher implements PasswordHasher {

    static final int STRENGTH = 10;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(STRENGTH);

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        return encoder.matches(rawPassword, hash);
    }
}
