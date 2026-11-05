package dev.gustavo.moirai.domain;

/** O projeto pedido não existe. */
public final class ProjectNotFoundException extends DomainException {

    private static final long serialVersionUID = 1L;

    public ProjectNotFoundException(ProjectId id) {
        super("Projeto %s não encontrado.".formatted(id));
    }
}
