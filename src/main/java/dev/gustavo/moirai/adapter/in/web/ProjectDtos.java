package dev.gustavo.moirai.adapter.in.web;

import dev.gustavo.moirai.domain.Project;
import dev.gustavo.moirai.domain.UserId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Contratos da API, separados do domínio: o domínio pode mudar sem quebrar quem consome a API. */
final class ProjectDtos {

    private ProjectDtos() {
    }

    record CreateProjectRequest(@NotBlank @Size(min = 3, max = 100) String name) {
    }

    record RenameProjectRequest(@NotBlank @Size(min = 3, max = 100) String name) {
    }

    record ProjectResponse(UUID id, String name, UUID ownerId, List<UUID> members) {

        static ProjectResponse from(Project project) {
            return new ProjectResponse(
                    project.id().value(),
                    project.name(),
                    project.ownerId().value(),
                    project.members().stream().map(UserId::value).sorted().toList());
        }
    }
}
