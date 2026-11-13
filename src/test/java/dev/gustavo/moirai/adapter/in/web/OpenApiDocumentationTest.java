package dev.gustavo.moirai.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Confere o contrato publicado: se um endpoint some ou perde a documentação de erro, o teste quebra. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Documentação OpenAPI")
class OpenApiDocumentationTest {

    private static final String DOCS = "/v3/api-docs";

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("publica todos os endpoints da API com título e versão")
    void publishesEndpoints() throws Exception {
        mvc.perform(get(DOCS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Moirai Taskflow API"))
                .andExpect(jsonPath("$.info.version").value(not("dev")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/projects")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/projects/{id}")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/projects/{projectId}/tasks")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/tasks/{id}/status")))
                .andExpect(jsonPath("$.paths", hasKey("/api/v1/tasks/{id}/assignee")));
    }

    @Test
    @DisplayName("documenta o access token como Bearer JWT, exigido em toda rota menos as de autenticação")
    void describesBearerToken() throws Exception {
        mvc.perform(get(DOCS))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.security[0]", hasKey("bearerAuth")))
                .andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.security", hasSize(0)))
                .andExpect(jsonPath("$.paths['/api/v1/projects'].post.tags", hasItem("Projetos")));
    }

    @Test
    @DisplayName("erros apontam para respostas ProblemDetail reutilizáveis")
    void documentsProblemResponses() throws Exception {
        mvc.perform(get(DOCS))
                .andExpect(jsonPath("$.paths['/api/v1/tasks/{id}/status'].patch.responses['422'].$ref")
                        .value("#/components/responses/UnprocessableContent"))
                .andExpect(jsonPath("$.components.responses.NotFound.content['application/problem+json'].schema.$ref")
                        .value("#/components/schemas/ProblemDetail"))
                .andExpect(jsonPath("$.components.schemas.ProblemDetail.properties", hasKey("errors")));
    }

    @Test
    @DisplayName("os contratos trazem exemplos e a listagem explica a ordenação")
    void documentsExamplesAndParameters() throws Exception {
        mvc.perform(get(DOCS))
                .andExpect(jsonPath("$.components.schemas.CreateTaskRequest.properties.dueDate.example").value("2099-12-31"))
                .andExpect(jsonPath("$.paths['/api/v1/projects/{projectId}/tasks'].get.parameters[?(@.name == 'sort')].description",
                        hasItem(containsString("desc"))));
    }

    @Test
    @DisplayName("o Swagger UI abre")
    void swaggerUi() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Swagger UI")));
    }
}
