package dev.gustavo.moirai.adapter.in.web;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.gustavo.moirai.application.port.in.ProjectUseCases.CreateProject;
import dev.gustavo.moirai.application.port.in.ProjectUseCases.DeleteProject;
import dev.gustavo.moirai.application.port.in.ProjectUseCases.GetProject;
import dev.gustavo.moirai.application.port.in.ProjectUseCases.ListProjects;
import dev.gustavo.moirai.application.port.in.ProjectUseCases.ManageMembers;
import dev.gustavo.moirai.application.port.in.ProjectUseCases.RenameProject;
import dev.gustavo.moirai.domain.ForbiddenOperationException;
import dev.gustavo.moirai.domain.Project;
import dev.gustavo.moirai.domain.ProjectId;
import dev.gustavo.moirai.domain.ProjectNotFoundException;
import dev.gustavo.moirai.domain.UserId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProjectController.class)
@Import({dev.gustavo.moirai.config.SecurityTestImports.class})
@DisplayName("ProjectController (@WebMvcTest)")
class ProjectControllerWebMvcTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CreateProject createProject;

    @MockitoBean
    private GetProject getProject;

    @MockitoBean
    private ListProjects listProjects;

    @MockitoBean
    private ManageMembers manageMembers;

    @MockitoBean
    private RenameProject renameProject;

    @MockitoBean
    private DeleteProject deleteProject;

    private final UserId owner = UserId.newId();

    @Test
    @DisplayName("lista os projetos do usuário do cabeçalho, com os membros")
    void list() throws Exception {
        Project project = Project.create("Moirai", owner);
        when(listProjects.listFor(owner)).thenReturn(List.of(project));

        mvc.perform(get("/api/v1/projects").with(TestUsers.as(owner.value())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Moirai"))
                .andExpect(jsonPath("$[0].members[0]").value(owner.value().toString()));
    }

    @Test
    @DisplayName("DELETE devolve 204, e o domínio recusando vira 403")
    void delete204And403() throws Exception {
        ProjectId mine = ProjectId.newId();
        ProjectId theirs = ProjectId.newId();
        doThrow(new ForbiddenOperationException("Só o dono pode excluir o projeto."))
                .when(deleteProject).delete(theirs, owner);

        mvc.perform(delete("/api/v1/projects/{id}", mine.value()).with(TestUsers.as(owner.value())))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/projects/{id}", theirs.value()).with(TestUsers.as(owner.value())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Só o dono pode excluir o projeto."));

        verify(deleteProject).delete(mine, owner);
    }

    @Test
    @DisplayName("id que não é UUID e projeto inexistente devolvem 400 e 404")
    void badIdAndNotFound() throws Exception {
        ProjectId missing = ProjectId.newId();
        when(getProject.get(missing, owner)).thenThrow(new ProjectNotFoundException(missing));

        mvc.perform(get("/api/v1/projects/{id}", "nao-e-uuid").with(TestUsers.as(owner.value())))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/projects/{id}", missing.value()).with(TestUsers.as(owner.value())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("/problems/not-found"));
    }
}
