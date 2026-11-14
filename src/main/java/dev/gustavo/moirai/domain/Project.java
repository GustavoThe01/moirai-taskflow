package dev.gustavo.moirai.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Projeto que agrupa tarefas e define quem pode trabalhar nelas.
 *
 * <p>Regras:
 * <ul>
 *   <li>o dono é sempre membro e não pode ser removido;</li>
 *   <li>só o dono adiciona ou remove membros;</li>
 *   <li>o nome tem entre 3 e 100 caracteres.</li>
 * </ul>
 */
public final class Project {

    private static final int NAME_MIN_LENGTH = 3;
    private static final int NAME_MAX_LENGTH = 100;

    private final ProjectId id;
    private final UserId ownerId;
    private final Set<UserId> members = new LinkedHashSet<>();
    private String name;

    private Project(ProjectId id, String name, UserId ownerId) {
        this.id = Objects.requireNonNull(id, "id");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.name = validateName(name);
        this.members.add(ownerId);
    }

    public static Project create(String name, UserId ownerId) {
        return new Project(ProjectId.newId(), name, ownerId);
    }

    /** Reconstrói um projeto já existente a partir de um adapter de persistência. */
    public static Project restore(ProjectId id, String name, UserId ownerId, Collection<UserId> members) {
        Project project = new Project(id, name, ownerId);
        project.members.addAll(members);
        return project;
    }

    public void addMember(UserId newMember, UserId actor) {
        Objects.requireNonNull(newMember, "newMember");
        requireOwner(actor, "adicionar membros");
        members.add(newMember);
    }

    public void removeMember(UserId member, UserId actor) {
        requireOwner(actor, "remover membros");
        if (ownerId.equals(member)) {
            throw new BusinessRuleViolationException("O dono não pode ser removido do próprio projeto.");
        }
        members.remove(member);
    }

    public void rename(String newName, UserId actor) {
        requireOwner(actor, "renomear o projeto");
        name = validateName(newName);
    }

    /** Excluir um projeto apaga as tarefas dele, então só o dono pode fazer isso. */
    public void requireCanBeDeletedBy(UserId actor) {
        requireOwner(actor, "excluir o projeto");
    }

    public boolean isMember(UserId userId) {
        return members.contains(userId);
    }

    /** Garante que o usuário é membro; usado pelas regras de {@link Task}. */
    public void requireMember(UserId userId) {
        if (!isMember(userId)) {
            throw new NotProjectMemberException(userId, id);
        }
    }

    public ProjectId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public UserId ownerId() {
        return ownerId;
    }

    public Set<UserId> members() {
        return Collections.unmodifiableSet(members);
    }

    /** Garante que quem age é o dono. A ação entra na mensagem: "Só o dono do projeto pode {ação}." */
    public void requireOwner(UserId actor, String action) {
        if (!ownerId.equals(actor)) {
            throw new ForbiddenOperationException("Só o dono do projeto pode %s.".formatted(action));
        }
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidValueException("O nome do projeto é obrigatório.");
        }
        String stripped = name.strip();
        if (stripped.length() < NAME_MIN_LENGTH || stripped.length() > NAME_MAX_LENGTH) {
            throw new InvalidValueException("O nome do projeto deve ter entre %d e %d caracteres."
                    .formatted(NAME_MIN_LENGTH, NAME_MAX_LENGTH));
        }
        return stripped;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Project project && id.equals(project.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
