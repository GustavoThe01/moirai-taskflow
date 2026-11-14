package dev.gustavo.moirai.application.port.in;

import dev.gustavo.moirai.domain.Project;
import dev.gustavo.moirai.domain.ProjectId;
import dev.gustavo.moirai.domain.User;
import dev.gustavo.moirai.domain.UserId;
import java.util.List;

/**
 * Portas de entrada: o que o mundo externo pode pedir à aplicação sobre projetos.
 *
 * <p>Interfaces pequenas, uma por caso de uso: quem só lê não depende de quem altera (ISP).
 */
public final class ProjectUseCases {

    private ProjectUseCases() {
    }

    public interface CreateProject {
        Project create(String name, UserId ownerId);
    }

    public interface GetProject {
        /** Só membros enxergam o projeto. */
        Project get(ProjectId id, UserId actor);
    }

    public interface ListProjects {
        List<Project> listFor(UserId member);
    }

    public interface RenameProject {
        Project rename(ProjectId id, String newName, UserId actor);
    }

    public interface DeleteProject {
        void delete(ProjectId id, UserId actor);
    }

    public interface ManageMembers {
        /** Só o dono adiciona. O novo membro precisa ter conta: é encontrado pelo e-mail. */
        Project addMember(ProjectId id, String email, UserId actor);

        /** Só o dono remove, e o dono não pode ser removido. */
        Project removeMember(ProjectId id, UserId member, UserId actor);

        /** Os membros com nome e e-mail. Só membros podem ver. */
        List<User> listMembers(ProjectId id, UserId actor);
    }
}
