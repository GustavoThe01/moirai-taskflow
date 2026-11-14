package dev.gustavo.moirai.adapter.in.web;

import dev.gustavo.moirai.application.port.in.ProjectUseCases.ManageMembers;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import dev.gustavo.moirai.adapter.in.web.ProjectDtos.CreateProjectRequest;
import dev.gustavo.moirai.adapter.in.web.ProjectDtos.ProjectResponse;
import dev.gustavo.moirai.adapter.in.web.ProjectDtos.RenameProjectRequest;
import dev.gustavo.moirai.application.port.in.ProjectUseCases.CreateProject;
import dev.gustavo.moirai.application.port.in.ProjectUseCases.DeleteProject;
import dev.gustavo.moirai.application.port.in.ProjectUseCases.GetProject;
import dev.gustavo.moirai.application.port.in.ProjectUseCases.ListProjects;
import dev.gustavo.moirai.application.port.in.ProjectUseCases.RenameProject;
import dev.gustavo.moirai.domain.Project;
import dev.gustavo.moirai.domain.ProjectId;
import dev.gustavo.moirai.domain.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de projetos.
 *
 * <p>O usuário é o dono do access token (claim {@code sub}), recebido pelo parâmetro {@link CurrentUser}.
 */
@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projetos")
class ProjectController {


    private final CreateProject createProject;
    private final GetProject getProject;
    private final ListProjects listProjects;
    private final RenameProject renameProject;
    private final DeleteProject deleteProject;
    private final ManageMembers manageMembers;

    ProjectController(CreateProject createProject, GetProject getProject, ListProjects listProjects,
                      RenameProject renameProject, DeleteProject deleteProject, ManageMembers manageMembers) {
        this.manageMembers = manageMembers;
        this.createProject = createProject;
        this.getProject = getProject;
        this.listProjects = listProjects;
        this.renameProject = renameProject;
        this.deleteProject = deleteProject;
    }

    @PostMapping
    @Operation(summary = "Cria um projeto", description = "Quem cria vira dono e primeiro membro do projeto.")
    @ApiResponse(responseCode = "201", description = "Projeto criado; o cabeçalho Location aponta para ele")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    ResponseEntity<ProjectResponse> create(@CurrentUser UserId user,
                                           @Valid @RequestBody CreateProjectRequest request) {
        Project project = createProject.create(request.name(), user);
        URI location = URI.create("/api/v1/projects/" + project.id());
        return ResponseEntity.created(location).body(ProjectResponse.from(project));
    }

    @GetMapping
    @Operation(summary = "Lista os projetos de que o usuário é membro")
    @ApiResponse(responseCode = "200", description = "Projetos do usuário, ordenados pelo nome")
    List<ProjectResponse> list(@CurrentUser UserId user) {
        return listProjects.listFor(user).stream().map(ProjectResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um projeto", description = "Só membros do projeto podem vê-lo.")
    @ApiResponse(responseCode = "200", description = "Projeto encontrado")
    @ApiResponse(responseCode = "403", ref = "Forbidden")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    ProjectResponse get(@CurrentUser UserId user, @PathVariable UUID id) {
        return ProjectResponse.from(getProject.get(new ProjectId(id), user));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Renomeia um projeto", description = "Só o dono pode renomear.")
    @ApiResponse(responseCode = "200", description = "Projeto com o nome novo")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "403", ref = "Forbidden")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    ProjectResponse rename(@CurrentUser UserId user, @PathVariable UUID id,
                           @Valid @RequestBody RenameProjectRequest request) {
        return ProjectResponse.from(renameProject.rename(new ProjectId(id), request.name(), user));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um projeto", description = "Só o dono pode excluir. As tarefas do projeto vão junto.")
    @ApiResponse(responseCode = "204", description = "Projeto excluído")
    @ApiResponse(responseCode = "403", ref = "Forbidden")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    void delete(@CurrentUser UserId user, @PathVariable UUID id) {
        deleteProject.delete(new ProjectId(id), user);
    }

    record AddMemberRequest(@Schema(example = "bia@exemplo.com") @NotBlank @Size(max = 254) String email) {
    }

    record MemberResponse(UUID id, String name, String email, boolean owner) {
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "Lista os membros do projeto", description = "Só membros podem ver. Ordenados pelo nome.")
    @ApiResponse(responseCode = "200", description = "Membros do projeto")
    @ApiResponse(responseCode = "403", ref = "Forbidden")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    List<MemberResponse> members(@CurrentUser UserId user, @PathVariable UUID id) {
        ProjectId projectId = new ProjectId(id);
        UserId owner = getProject.get(projectId, user).ownerId();
        return manageMembers.listMembers(projectId, user).stream()
                .map(member -> new MemberResponse(member.id().value(), member.name(), member.email(), member.id().equals(owner)))
                .toList();
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Adiciona um membro pelo e-mail", description = "Só o dono pode adicionar. A pessoa precisa ter conta.")
    @ApiResponse(responseCode = "200", description = "Projeto com o membro novo")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "403", ref = "Forbidden")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    ProjectResponse addMember(@CurrentUser UserId user, @PathVariable UUID id, @Valid @RequestBody AddMemberRequest request) {
        return ProjectResponse.from(manageMembers.addMember(new ProjectId(id), request.email(), user));
    }

    @DeleteMapping("/{id}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um membro", description = "Só o dono pode remover, e o dono não pode ser removido.")
    @ApiResponse(responseCode = "204", description = "Membro removido")
    @ApiResponse(responseCode = "403", ref = "Forbidden")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "422", ref = "UnprocessableContent")
    void removeMember(@CurrentUser UserId user, @PathVariable UUID id, @PathVariable UUID memberId) {
        manageMembers.removeMember(new ProjectId(id), new UserId(memberId), user);
    }
}
