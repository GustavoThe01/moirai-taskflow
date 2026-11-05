package dev.gustavo.moirai.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Atravessa todas as camadas: HTTP, caso de uso, domínio, JPA e SQLite. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("API de projetos")
class ProjectApiIntegrationTest {

    private static final String USER = "X-User-Id";

    @Autowired
    private MockMvc mvc;

    private final String owner = UUID.randomUUID().toString();
    private final String stranger = UUID.randomUUID().toString();

    private String createProject(String name) throws Exception {
        String location = mvc.perform(post("/api/v1/projects")
                        .header(USER, owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"%s\"}".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        assertThat(location).isNotNull();
        return location;
    }

    @Test
    @DisplayName("POST cria o projeto, com o criador como membro, e devolve 201 com Location")
    void createsProject() throws Exception {
        mvc.perform(post("/api/v1/projects")
                        .header(USER, owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Lançamento\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/projects/")))
                .andExpect(jsonPath("$.name").value("Lançamento"))
                .andExpect(jsonPath("$.ownerId").value(owner))
                .andExpect(jsonPath("$.members", contains(owner)));
    }

    @Test
    @DisplayName("GET lista só os projetos do usuário")
    void listsOnlyMyProjects() throws Exception {
        createProject("Meu projeto");

        mvc.perform(get("/api/v1/projects").header(USER, owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[*].name", hasItem("Meu projeto")));

        mvc.perform(get("/api/v1/projects").header(USER, stranger))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("GET por id: 200 para membro, 403 para quem não é membro e 404 para id inexistente")
    void getsById() throws Exception {
        String location = createProject("Meu projeto");

        mvc.perform(get(location).header(USER, owner)).andExpect(status().isOk());
        mvc.perform(get(location).header(USER, stranger)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/projects/" + UUID.randomUUID()).header(USER, owner))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", containsString("não encontrado")));
    }

    @Test
    @DisplayName("PUT renomeia para o dono e recusa quem não é dono")
    void renames() throws Exception {
        String location = createProject("Nome antigo");
        String body = "{\"name\": \"Nome novo\"}";

        mvc.perform(put(location).header(USER, stranger).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(put(location).header(USER, owner).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Nome novo"));
        mvc.perform(get(location).header(USER, owner)).andExpect(jsonPath("$.name").value("Nome novo"));
    }

    @Test
    @DisplayName("DELETE exclui para o dono e recusa quem não é dono")
    void deletes() throws Exception {
        String location = createProject("Descartável");

        mvc.perform(delete(location).header(USER, stranger)).andExpect(status().isForbidden());
        mvc.perform(delete(location).header(USER, owner)).andExpect(status().isNoContent());
        mvc.perform(get(location).header(USER, owner)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("nome inválido devolve 400")
    void rejectsInvalidName() throws Exception {
        mvc.perform(post("/api/v1/projects")
                        .header(USER, owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"ab\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("sem o cabeçalho do usuário ou com id mal formado devolve 400")
    void rejectsMissingOrInvalidUser() throws Exception {
        mvc.perform(get("/api/v1/projects")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/projects").header(USER, "nao-e-uuid")).andExpect(status().isBadRequest());
    }
}
