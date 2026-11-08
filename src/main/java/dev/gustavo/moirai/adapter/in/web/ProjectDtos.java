package dev.gustavo.moirai.adapter.in.web;

import dev.gustavo.moirai.domain.Project;
import dev.gustavo.moirai.domain.UserId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Contratos da API, separados do domínio: o domínio pode mudar sem quebrar quem consome a API. */
final class ProjectDtos {

    private ProjectDtos() {
    }

    record CreateProjectRequest(
            @Schema(description = "Nome do projeto", example = "Lançamento do app") @NotBlank @Size(min = 3, max = 100) String name) {
    }

    record RenameProjectRequest(
            @Schema(description = "Nome novo", example = "Lançamento do app 2.0") @NotBlank @Size(min = 3, max = 100) String name) {
    }

    record ProjectResponse(
            UUID id,
            @Schema(example = "Lançamento do app") String name,
            @Schema(description = "Dono do projeto: o único que pode renomear, excluir e gerenciar membros") UUID ownerId,
            @Schema(description = "Membros, incluindo o dono") List<UUID> members) {

        static ProjectResponse from(Project project) {
            return new ProjectResponse(
                    project.id().value(),
                    project.name(),
                    project.ownerId().value(),
                    project.members().stream().map(UserId::value).sorted().toList());
        }
    }
}
