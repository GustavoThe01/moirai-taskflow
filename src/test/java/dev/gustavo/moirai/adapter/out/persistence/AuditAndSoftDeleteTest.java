package dev.gustavo.moirai.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Lê as colunas direto do banco: a auditoria e a exclusão lógica não aparecem na API, só nas linhas. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Auditoria e exclusão lógica")
class AuditAndSoftDeleteTest {

    private static final String USER = "X-User-Id";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final String owner = UUID.randomUUID().toString();

    private String send(MockHttpServletRequestBuilder request, String body) throws Exception {
        return mvc.perform(request.header(USER, owner).contentType(MediaType.APPLICATION_JSON).content(body))
                .andReturn().getResponse().getHeader("Location");
    }

    private Map<String, Object> projectRow(String projectPath) {
        String id = projectPath.substring(projectPath.lastIndexOf('/') + 1);
        return jdbc.queryForMap("SELECT created_at, created_by, updated_at, updated_by, deleted_at FROM project WHERE id = ?", id);
    }

    @Test
    @DisplayName("criar e renomear registram quem e quando, sem perder quem criou")
    void auditsProject() throws Exception {
        String project = send(post("/api/v1/projects"), "{\"name\": \"Auditoria\"}");

        Map<String, Object> created = projectRow(project);
        assertThat(created.get("created_by")).isEqualTo(owner);
        assertThat(created.get("created_at")).isNotNull();
        assertThat(created.get("updated_by")).isEqualTo(owner);

        mvc.perform(put(project).header(USER, owner).contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"Auditoria 2\"}"))
                .andExpect(status().isOk());

        Map<String, Object> renamed = projectRow(project);
        assertThat(renamed.get("created_by")).isEqualTo(owner);
        assertThat(renamed.get("created_at")).isEqualTo(created.get("created_at"));
        assertThat(renamed.get("updated_by")).isEqualTo(owner);
    }

    @Test
    @DisplayName("alterar uma tarefa grava a última alteração na linha da tarefa")
    void auditsTask() throws Exception {
        String project = send(post("/api/v1/projects"), "{\"name\": \"Tarefas auditadas\"}");
        String task = send(post(project + "/tasks"), "{\"title\": \"Auditar\", \"priority\": \"LOW\"}");
        mvc.perform(patch(task + "/assignee").header(USER, owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"assigneeId\": \"%s\"}".formatted(owner))).andExpect(status().isOk());

        String id = task.substring(task.lastIndexOf('/') + 1);
        Map<String, Object> row = jdbc.queryForMap("SELECT updated_at, updated_by FROM task WHERE id = ?", id);
        assertThat(row.get("updated_by")).isEqualTo(owner);
        assertThat(row.get("updated_at")).isNotNull();
    }

    @Test
    @DisplayName("excluir o projeto só marca a data: some da API, mas a linha e as tarefas continuam no banco")
    void softDelete() throws Exception {
        String project = send(post("/api/v1/projects"), "{\"name\": \"Vai sumir\"}");
        String task = send(post(project + "/tasks"), "{\"title\": \"Tarefa órfã\", \"priority\": \"LOW\"}");

        mvc.perform(delete(project).header(USER, owner)).andExpect(status().isNoContent());

        mvc.perform(get(project).header(USER, owner)).andExpect(status().isNotFound());
        mvc.perform(get(project + "/tasks").header(USER, owner)).andExpect(status().isNotFound());
        mvc.perform(patch(task + "/status").header(USER, owner).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"IN_PROGRESS\"}")).andExpect(status().isNotFound());
        String listed = mvc.perform(get("/api/v1/projects").header(USER, owner)).andReturn().getResponse().getContentAsString();
        assertThat(listed).doesNotContain("Vai sumir");

        assertThat(projectRow(project).get("deleted_at")).isNotNull();
        String taskId = task.substring(task.lastIndexOf('/') + 1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM task WHERE id = ?", Integer.class, taskId)).isEqualTo(1);
    }

    @Test
    @DisplayName("excluir de novo não muda a data da primeira exclusão")
    void deleteTwice() throws Exception {
        String project = send(post("/api/v1/projects"), "{\"name\": \"Duas vezes\"}");
        mvc.perform(delete(project).header(USER, owner)).andExpect(status().isNoContent());
        Object firstDeletion = projectRow(project).get("deleted_at");

        mvc.perform(delete(project).header(USER, owner)).andExpect(status().isNotFound());

        assertThat(projectRow(project).get("deleted_at")).isEqualTo(firstDeletion);
    }
}
