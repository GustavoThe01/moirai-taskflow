package dev.gustavo.moirai.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Etiqueta de um projeto, como "bug" ou "urgente". Pertence ao projeto e só pode marcar tarefas dele.
 *
 * <p>O nome é único dentro do projeto sem diferenciar maiúsculas; quem garante a unicidade é o banco
 * (índice único), porque o domínio não conhece as outras etiquetas.
 */
public final class Label {

    private static final int NAME_MAX_LENGTH = 30;
    private static final Pattern COLOR = Pattern.compile("#[0-9A-F]{6}");

    private final LabelId id;
    private final ProjectId projectId;
    private final String name;
    private final String color;

    private Label(LabelId id, ProjectId projectId, String name, String color) {
        this.id = Objects.requireNonNull(id, "id");
        this.projectId = Objects.requireNonNull(projectId, "projectId");
        this.name = validateName(name);
        this.color = validateColor(color);
    }

    public static Label create(Project project, UserId actor, String name, String color) {
        Objects.requireNonNull(project, "project");
        project.requireMember(actor);
        return new Label(LabelId.newId(), project.id(), name, color);
    }

    public static Label restore(LabelId id, ProjectId projectId, String name, String color) {
        return new Label(id, projectId, name, color);
    }

    public LabelId id() {
        return id;
    }

    public ProjectId projectId() {
        return projectId;
    }

    public String name() {
        return name;
    }

    /** Cor em hexadecimal maiúsculo, como #E11D48. */
    public String color() {
        return color;
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidValueException("O nome da etiqueta é obrigatório.");
        }
        String stripped = name.strip();
        if (stripped.length() > NAME_MAX_LENGTH) {
            throw new InvalidValueException("O nome da etiqueta tem no máximo %d caracteres.".formatted(NAME_MAX_LENGTH));
        }
        return stripped;
    }

    private static String validateColor(String color) {
        String normalized = color == null ? "" : color.strip().toUpperCase(Locale.ROOT);
        if (!COLOR.matcher(normalized).matches()) {
            throw new InvalidValueException("A cor deve estar no formato #RRGGBB: " + color);
        }
        return normalized;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Label label && id.equals(label.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
