package dev.gustavo.moirai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.gustavo.moirai.application.port.out.PasswordHasher;
import dev.gustavo.moirai.application.port.out.UserRepository;
import dev.gustavo.moirai.domain.EmailAlreadyRegisteredException;
import dev.gustavo.moirai.domain.InvalidCredentialsException;
import dev.gustavo.moirai.domain.InvalidValueException;
import dev.gustavo.moirai.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Cadastro e login sem Spring, sem banco e sem BCrypt: as duas portas de saída são mocks. */
@DisplayName("UserService")
class UserServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-12T12:00:00Z");

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final UserService service;

    UserServiceTest() {
        when(hasher.hash(anyString())).thenAnswer(call -> "hash:" + call.getArgument(0));
        service = new UserService(users, hasher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private User existing(String email, String password) {
        User user = User.register(email, "Ana", "hash:" + password, NOW);
        when(users.findByEmail(email)).thenReturn(Optional.of(user));
        when(hasher.matches(password, "hash:" + password)).thenReturn(true);
        return user;
    }

    @Test
    @DisplayName("cadastrar grava o hash, nunca a senha")
    void registerStoresHash() {
        User user = service.register("Ana@Exemplo.com", "Ana", "senha-longa");

        verify(users).save(user);
        assertThat(user.email()).isEqualTo("ana@exemplo.com");
        assertThat(user.passwordHash()).isEqualTo("hash:senha-longa");
        assertThat(user.createdAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("e-mail já cadastrado, com outra caixa, é recusado")
    void duplicateEmail() {
        when(users.existsByEmail("ana@exemplo.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register("ANA@exemplo.com", "Ana", "senha-longa"))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("senha com menos de 8 caracteres ou mais de 72 bytes é recusada")
    void passwordLimits() {
        assertThatThrownBy(() -> service.register("ana@exemplo.com", "Ana", "curta"))
                .isInstanceOf(InvalidValueException.class);
        // 37 caracteres "ç" ocupam 74 bytes em UTF-8: passa no limite de caracteres, mas não no do BCrypt.
        assertThatThrownBy(() -> service.register("ana@exemplo.com", "Ana", "ç".repeat(37)))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("72 bytes");
        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("login com e-mail e senha certos devolve o usuário, sem diferenciar a caixa do e-mail")
    void loginOk() {
        User user = existing("ana@exemplo.com", "senha-longa");

        assertThat(service.authenticate(" ANA@exemplo.com ", "senha-longa")).isEqualTo(user);
    }

    @Test
    @DisplayName("senha errada e e-mail desconhecido dão o mesmo erro")
    void sameErrorForBothCases() {
        existing("ana@exemplo.com", "senha-longa");
        when(users.findByEmail("ninguem@exemplo.com")).thenReturn(Optional.empty());

        Throwable wrongPassword = org.assertj.core.api.Assertions.catchThrowable(
                () -> service.authenticate("ana@exemplo.com", "outra-senha"));
        Throwable unknownEmail = org.assertj.core.api.Assertions.catchThrowable(
                () -> service.authenticate("ninguem@exemplo.com", "senha-longa"));

        assertThat(wrongPassword).isInstanceOf(InvalidCredentialsException.class);
        assertThat(unknownEmail).isInstanceOf(InvalidCredentialsException.class)
                .hasMessage(wrongPassword.getMessage());
    }

    @Test
    @DisplayName("com e-mail desconhecido, o BCrypt roda mesmo assim, para o tempo de resposta não denunciar")
    void hashesEvenWithoutUser() {
        when(users.findByEmail("ninguem@exemplo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate("ninguem@exemplo.com", "senha-longa"))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(hasher).matches(eq("senha-longa"), eq("hash:moirai-sem-usuario"));
    }

    @Test
    @DisplayName("e-mail mal formado no login é só credencial inválida, sem detalhar")
    void malformedEmailOnLogin() {
        assertThatThrownBy(() -> service.authenticate("isso não é e-mail", "senha-longa"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
