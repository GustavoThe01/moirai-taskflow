package dev.gustavo.moirai.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("User")
class UserTest {

    private static final Instant NOW = Instant.parse("2026-11-12T12:00:00Z");

    @Test
    @DisplayName("o e-mail é gravado sem espaços e em minúsculas")
    void normalizesEmail() {
        User user = User.register("  Ana.Lima@Exemplo.COM ", " Ana Lima ", "$2a$10$hash", NOW);

        assertThat(user.email()).isEqualTo("ana.lima@exemplo.com");
        assertThat(user.name()).isEqualTo("Ana Lima");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "sem-arroba", "ana@", "@exemplo.com", "ana@exemplo", "ana lima@exemplo.com"})
    @DisplayName("e-mail sem a forma algo@algo.algo é recusado")
    void rejectsInvalidEmail(String email) {
        assertThatThrownBy(() -> User.register(email, "Ana", "$2a$10$hash", NOW)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("nome vazio ou com mais de 100 caracteres é recusado")
    void rejectsInvalidName() {
        assertThatThrownBy(() -> User.register("ana@exemplo.com", " ", "$2a$10$hash", NOW))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> User.register("ana@exemplo.com", "a".repeat(101), "$2a$10$hash", NOW))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("o toString não mostra o hash da senha")
    void toStringHidesHash() {
        User user = User.register("ana@exemplo.com", "Ana", "$2a$10$segredo", NOW);

        assertThat(user.toString()).contains("ana@exemplo.com").doesNotContain("segredo");
    }
}
