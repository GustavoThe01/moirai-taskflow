package dev.gustavo.moirai.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("API de tarefas e erros padronizados")
class TaskApiIntegrationTest {

    private static final String USER = "X-User-Id";
    private static final MediaType PROBLEM = MediaType.APPLICATION_PROBLEM_JSON;

    @Autowired
    private MockMvc mvc;

    private final String owner = UUID.randomUUID().toString();
    private final String stranger = UUID.randomUUID().toString();
    private String projectPath;

    @BeforeEach
    void createProject() throws Exception {
        projectPath = mvc.perform(post("/api/v1/projects")
                        .header(USER, owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Moirai\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        assertThat(projectPath).isNotNull();
    }

    private ResultActions postTask(String user, String body) throws Exception {
        return mvc.perform(post(projectPath + "/tasks")
                .header(USER, user)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String createTask() throws Exception {
        String location = postTask(owner, "{\"title\": \"Escrever testes\", \"priority\": \"HIGH\"}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        assertThat(location).isNotNull();
        return location;
    }

    private ResultActions patchJson(String path, String user, String body) throws Exception {
        return mvc.perform(patch(path).header(USER, user).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions moveTo(String task, String user, String target) throws Exception {
        return patchJson(task + "/status", user, "{\"status\": \"%s\"}".formatted(target));
    }

    private void assignToOwner(String task) throws Exception {
        patchJson(task + "/assignee", owner, "{\"assigneeId\": \"%s\"}".formatted(owner)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST cria a tarefa em TODO, sem responsável")
    void createsTask() throws Exception {
        postTask(owner, "{\"title\": \"Escrever testes\", \"priority\": \"HIGH\", \"dueDate\": \"2099-12-31\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.dueDate").value("2099-12-31"))
                .andExpect(jsonPath("$.assigneeId").doesNotExist())
                .andExpect(jsonPath("$.createdBy").value(owner));
    }

    @Test
    @DisplayName("a tarefa percorre o fluxo completo até DONE")
    void fullFlow() throws Exception {
        String task = createTask();
        assignToOwner(task);

        moveTo(task, owner, "IN_PROGRESS").andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        moveTo(task, owner, "REVIEW").andExpect(jsonPath("$.status").value("REVIEW"));
        moveTo(task, owner, "DONE").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    @DisplayName("pular etapa devolve 422 com ProblemDetail e o caminho da requisição")
    void invalidTransition() throws Exception {
        String task = createTask();

        moveTo(task, owner, "DONE")
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.type").value("/problems/invalid-transition"))
                .andExpect(jsonPath("$.title").value("Transição de status inválida"))
                .andExpect(jsonPath("$.detail").value("Uma tarefa em TODO não pode ir para DONE."))
                .andExpect(jsonPath("$.instance", endsWith("/status")));
    }

    @Test
    @DisplayName("iniciar sem responsável devolve 422 de regra de negócio")
    void startWithoutAssignee() throws Exception {
        moveTo(createTask(), owner, "IN_PROGRESS")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.type").value("/problems/business-rule"));
    }

    @Test
    @DisplayName("quem não é membro recebe 403")
    void strangerIsForbidden() throws Exception {
        postTask(stranger, "{\"title\": \"Invasão\", \"priority\": \"LOW\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/forbidden"));

        moveTo(createTask(), stranger, "IN_PROGRESS").andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("id inexistente devolve 404")
    void notFound() throws Exception {
        moveTo("/api/v1/tasks/" + UUID.randomUUID(), owner, "IN_PROGRESS")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("/problems/not-found"));
    }

    @Test
    @DisplayName("campos inválidos devolvem 400 listando cada campo")
    void validationErrors() throws Exception {
        postTask(owner, "{\"title\": \"ab\"}")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.type").value("/problems/validation"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("title")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("priority")));
    }

    @Test
    @DisplayName("prazo no passado é recusado pelo domínio com 400")
    void pastDueDate() throws Exception {
        postTask(owner, "{\"title\": \"Atrasada\", \"priority\": \"LOW\", \"dueDate\": \"2000-01-01\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problems/invalid-value"));
    }

    @Test
    @DisplayName("JSON mal formado e status desconhecido também viram ProblemDetail")
    void malformedRequests() throws Exception {
        postTask(owner, "{ isto não é json")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM));

        moveTo(createTask(), owner, "ARQUIVADA")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM));
    }
}
