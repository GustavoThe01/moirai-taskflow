package dev.gustavo.moirai.adapter.in.web;

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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de projetos.
 *
 * <p>O usuário vem do cabeçalho {@code X-User-Id} apenas até o Dia 12, quando o JWT o substitui.
 */
@RestController
@RequestMapping("/api/v1/projects")
class ProjectController {

    private static final String USER_HEADER = "X-User-Id";

    private final CreateProject createProject;
    private final GetProject getProject;
    private final ListProjects listProjects;
    private final RenameProject renameProject;
    private final DeleteProject deleteProject;

    ProjectController(CreateProject createProject, GetProject getProject, ListProjects listProjects,
                      RenameProject renameProject, DeleteProject deleteProject) {
        this.createProject = createProject;
        this.getProject = getProject;
        this.listProjects = listProjects;
        this.renameProject = renameProject;
        this.deleteProject = deleteProject;
    }

    @PostMapping
    ResponseEntity<ProjectResponse> create(@RequestHeader(USER_HEADER) UUID user,
                                           @Valid @RequestBody CreateProjectRequest request) {
        Project project = createProject.create(request.name(), new UserId(user));
        URI location = URI.create("/api/v1/projects/" + project.id());
        return ResponseEntity.created(location).body(ProjectResponse.from(project));
    }

    @GetMapping
    List<ProjectResponse> list(@RequestHeader(USER_HEADER) UUID user) {
        return listProjects.listFor(new UserId(user)).stream().map(ProjectResponse::from).toList();
    }

    @GetMapping("/{id}")
    ProjectResponse get(@RequestHeader(USER_HEADER) UUID user, @PathVariable UUID id) {
        return ProjectResponse.from(getProject.get(new ProjectId(id), new UserId(user)));
    }

    @PutMapping("/{id}")
    ProjectResponse rename(@RequestHeader(USER_HEADER) UUID user, @PathVariable UUID id,
                           @Valid @RequestBody RenameProjectRequest request) {
        return ProjectResponse.from(renameProject.rename(new ProjectId(id), request.name(), new UserId(user)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@RequestHeader(USER_HEADER) UUID user, @PathVariable UUID id) {
        deleteProject.delete(new ProjectId(id), new UserId(user));
    }
}
