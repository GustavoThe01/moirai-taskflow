package dev.gustavo.moirai.application.service;

import dev.gustavo.moirai.application.port.in.UserUseCases.AuthenticateUser;
import dev.gustavo.moirai.application.port.in.UserUseCases.RegisterUser;
import dev.gustavo.moirai.application.port.out.PasswordHasher;
import dev.gustavo.moirai.application.port.out.UserRepository;
import dev.gustavo.moirai.domain.EmailAlreadyRegisteredException;
import dev.gustavo.moirai.domain.InvalidCredentialsException;
import dev.gustavo.moirai.domain.InvalidValueException;
import dev.gustavo.moirai.domain.User;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UserService implements RegisterUser, AuthenticateUser {

    static final int PASSWORD_MIN_LENGTH = 8;
    // O BCrypt só usa os primeiros 72 bytes da senha; o resto seria ignorado sem aviso.
    static final int PASSWORD_MAX_BYTES = 72;

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final Clock clock;
    // Hash de uma senha qualquer, usado quando o e-mail não existe (ver authenticate).
    private final String dummyHash;

    UserService(UserRepository users, PasswordHasher hasher, Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.clock = clock;
        this.dummyHash = hasher.hash("moirai-sem-usuario");
    }

    @Override
    @Transactional
    public User register(String email, String name, String password) {
        validatePassword(password);
        String normalized = User.normalizeEmail(email);
        // Mensagem clara para o caso comum. O índice único do banco continua sendo a garantia final.
        if (users.existsByEmail(normalized)) {
            throw new EmailAlreadyRegisteredException(normalized);
        }
        User user = User.register(normalized, name, hasher.hash(password), clock.instant());
        users.save(user);
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public User authenticate(String email, String password) {
        Optional<User> user = findByEmail(email);
        // Mesmo sem usuário, o BCrypt roda contra um hash qualquer. Assim a resposta demora o mesmo tempo
        // para e-mail desconhecido e para senha errada, e o tempo não revela quais e-mails têm conta.
        String hash = user.map(User::passwordHash).orElse(dummyHash);
        boolean matches = password != null && hasher.matches(password, hash);
        if (user.isEmpty() || !matches) {
            throw new InvalidCredentialsException();
        }
        return user.get();
    }

    private Optional<User> findByEmail(String email) {
        try {
            return users.findByEmail(User.normalizeEmail(email));
        } catch (InvalidValueException exception) {
            return Optional.empty();
        }
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < PASSWORD_MIN_LENGTH) {
            throw new InvalidValueException("A senha precisa ter pelo menos %d caracteres.".formatted(PASSWORD_MIN_LENGTH));
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > PASSWORD_MAX_BYTES) {
            throw new InvalidValueException("A senha pode ter no máximo %d bytes.".formatted(PASSWORD_MAX_BYTES));
        }
    }
}
