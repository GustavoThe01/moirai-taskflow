package dev.gustavo.moirai.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
@DisplayName("API de etiquetas e comentários")
class LabelAndCommentApiTest {

    private static final String USER = "X-User-Id";

    @Autowired
    private MockMvc mvc;

    private final String owner = UUID.randomUUID().toString();
    private String projectPath;
    private String taskPath;

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String user, String body) throws Exception {
        return mvc.perform(request.header(USER, user).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @BeforeEach
    void createProjectAndTask() throws Exception {
        projectPath = send(post("/api/v1/projects"), owner, "{\"name\": \"Moirai\"}")
                .andReturn().getResponse().getHeader("Location");
        taskPath = send(post(projectPath + "/tasks"), owner, "{\"title\": \"Corrigir login\", \"priority\": \"HIGH\"}")
                .andReturn().getResponse().getHeader("Location");
        assertThat(taskPath).isNotNull();
    }

    private String createLabel(String name) throws Exception {
        String body = send(post(projectPath + "/labels"), owner, "{\"name\": \"%s\", \"color\": \"#e11d48\"}".formatted(name))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    @DisplayName("cria etiquetas, lista em ordem de nome e recusa nome repetido sem diferenciar maiúsculas")
    void labels() throws Exception {
        createLabel("urgente");
        createLabel("bug");

        mvc.perform(get(projectPath + "/labels").header(USER, owner))
                .andExpect(jsonPath("$[*].name", contains("bug", "urgente")))
                .andExpect(jsonPath("$[0].color").value("#E11D48"));
        send(post(projectPath + "/labels"), owner, "{\"name\": \"BUG\", \"color\": \"#000000\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("O projeto já tem a etiqueta \"BUG\"."));
    }

    @Test
    @DisplayName("troca as etiquetas da tarefa e devolve os ids na resposta")
    void relabelTask() throws Exception {
        String bug = createLabel("bug");
        String urgent = createLabel("urgente");

        send(put(taskPath + "/labels"), owner, "{\"labelIds\": [\"%s\", \"%s\"]}".formatted(bug, urgent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.labelIds", containsInAnyOrder(bug, urgent)));
        mvc.perform(get(projectPath + "/tasks").header(USER, owner))
                .andExpect(jsonPath("$.items[0].labelIds", hasSize(2)));
        send(put(taskPath + "/labels"), owner, "{\"labelIds\": []}")
                .andExpect(jsonPath("$.labelIds", hasSize(0)));
    }

    @Test
    @DisplayName("etiqueta inexistente é 400, e etiqueta de outro projeto é 422")
    void relabelErrors() throws Exception {
        send(put(taskPath + "/labels"), owner, "{\"labelIds\": [\"%s\"]}".formatted(UUID.randomUUID()))
                .andExpect(status().isBadRequest());

        String otherProject = send(post("/api/v1/projects"), owner, "{\"name\": \"Outro\"}")
                .andReturn().getResponse().getHeader("Location");
        String foreign = JsonPath.read(send(post(otherProject + "/labels"), owner, "{\"name\": \"bug\", \"color\": \"#000000\"}")
                .andReturn().getResponse().getContentAsString(), "$.id");
        send(put(taskPath + "/labels"), owner, "{\"labelIds\": [\"%s\"]}".formatted(foreign))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    @DisplayName("comentários em ordem de criação, e quem não é membro recebe 403")
    void comments() throws Exception {
        send(post(taskPath + "/comments"), owner, "{\"text\": \"Comecei pela tela de login.\"}").andExpect(status().isCreated());
        send(post(taskPath + "/comments"), owner, "{\"text\": \"Falta o teste.\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorId").value(owner));

        mvc.perform(get(taskPath + "/comments").header(USER, owner))
                .andExpect(jsonPath("$[*].text", contains("Comecei pela tela de login.", "Falta o teste.")));
        send(post(taskPath + "/comments"), UUID.randomUUID().toString(), "{\"text\": \"Intruso\"}")
                .andExpect(status().isForbidden());
        send(post(taskPath + "/comments"), owner, "{\"text\": \"\"}")
                .andExpect(status().isBadRequest());
    }
}
