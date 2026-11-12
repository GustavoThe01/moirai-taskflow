package dev.gustavo.moirai.application.port.out;

/**
 * Porta de saída: transforma uma senha em hash e confere uma senha contra um hash.
 *
 * <p>A aplicação não sabe que o algoritmo é o BCrypt. Trocar de algoritmo é trocar o adapter.
 */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hash);
}
