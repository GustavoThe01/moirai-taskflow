package dev.gustavo.moirai.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Pessoa que usa o Moirai. O e-mail é o login e é único (sem diferenciar maiúsculas).
 *
 * <p>O domínio guarda o hash da senha como um texto opaco: não sabe qual algoritmo o gerou nem como conferir.
 * Isso é trabalho da porta {@code PasswordHasher}, implementada com BCrypt fora do domínio.
 */
public final class User {

    private static final int EMAIL_MAX_LENGTH = 254;
    private static final int NAME_MAX_LENGTH = 100;
    // Simples de propósito: confere a forma (algo@algo.algo). Se o e-mail existe, só um e-mail de confirmação diria.
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private final UserId id;
    private final String email;
    private final String name;
    private final String passwordHash;
    private final Role role;
    private final Instant createdAt;

    private User(UserId id, String email, String name, String passwordHash, Role role, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.email = normalizeEmail(email);
        this.name = validateName(name);
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.role = Objects.requireNonNull(role, "role");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static User register(String email, String name, String passwordHash, Instant now) {
        return register(email, name, passwordHash, Role.USER, now);
    }

    public static User register(String email, String name, String passwordHash, Role role, Instant now) {
        return new User(UserId.newId(), email, name, passwordHash, role, now);
    }

    public static User restore(UserId id, String email, String name, String passwordHash, Role role, Instant createdAt) {
        return new User(id, email, name, passwordHash, role, createdAt);
    }

    /** Forma canônica do e-mail: sem espaços nas pontas e em minúsculas. É assim que ele é gravado e buscado. */
    public static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidValueException("O e-mail é obrigatório.");
        }
        String normalized = email.strip().toLowerCase(Locale.ROOT);
        if (normalized.length() > EMAIL_MAX_LENGTH || !EMAIL.matcher(normalized).matches()) {
            throw new InvalidValueException("E-mail inválido: " + email.strip());
        }
        return normalized;
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidValueException("O nome é obrigatório.");
        }
        String stripped = name.strip();
        if (stripped.length() > NAME_MAX_LENGTH) {
            throw new InvalidValueException("O nome tem no máximo %d caracteres.".formatted(NAME_MAX_LENGTH));
        }
        return stripped;
    }

    public UserId id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String name() {
        return name;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Role role() {
        return role;
    }

    public Instant createdAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof User user && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /** Nunca inclui o hash da senha, para ele não parar num log por acidente. */
    @Override
    public String toString() {
        return "User[id=%s, email=%s]".formatted(id, email);
    }
}
