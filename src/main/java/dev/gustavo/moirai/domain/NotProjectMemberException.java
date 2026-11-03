package dev.gustavo.moirai.domain;

/** O usuário tentou agir sobre um projeto do qual não é membro. */
public final class NotProjectMemberException extends DomainException {

    private static final long serialVersionUID = 1L;

    public NotProjectMemberException(UserId userId, ProjectId projectId) {
        super("O usuário %s não é membro do projeto %s.".formatted(userId, projectId));
    }
}
