package dev.gustavo.moirai.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Autorização por recurso com usuários e tokens de verdade: quem é dono, quem é membro, quem não é nada, e a diferença
 * entre 401 (não sei quem você é) e 403 (sei quem você é, e você não pode).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Autorização por recurso")
class ResourceAuthorizationTest {

    private static final String PASSWORD = "senha-bem-longa";

    @Autowired
    private MockMvc mvc;

    private Account ana;
    private Account bia;
    private Account carla;
    private String project;
    private String task;

    private record Account(String id, String email, String token) {
    }

    private Account signUp(String name) throws Exception {
        String email = name.toLowerCase() + "." + UUID.randomUUID() + "@exemplo.com";
        json(post("/api/v1/auth/register"), null, "{\"email\": \"%s\", \"name\": \"%s\", \"password\": \"%s\"}".formatted(email, name, PASSWORD))
                .andExpect(status().isCreated());
        String login = json(post("/api/v1/auth/login"), null, "{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, PASSWORD))
                .andReturn().getResponse().getContentAsString();
        return new Account(JsonPath.read(login, "$.user.id"), email, JsonPath.read(login, "$.accessToken"));
    }

    private ResultActions json(MockHttpServletRequestBuilder request, Account as, String body) throws Exception {
        if (as != null) {
            request.header("Authorization", "Bearer " + as.token());
        }
        return mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions call(MockHttpServletRequestBuilder request, Account as) throws Exception {
        return json(request, as, "{}");
    }

    @BeforeEach
    void anaCreatesAProjectWithATask() throws Exception {
        ana = signUp("Ana");
        bia = signUp("Bia");
        carla = signUp("Carla");
        project = json(post("/api/v1/projects"), ana, "{\"name\": \"Lançamento\"}").andReturn().getResponse().getHeader("Location");
        task = json(post(project + "/tasks"), ana, "{\"title\": \"Escrever o anúncio\", \"priority\": \"HIGH\"}")
                .andReturn().getResponse().getHeader("Location");
    }

    @Test
    @DisplayName("o dono adiciona pelo e-mail, e o novo membro passa a enxergar o projeto e as tarefas")
    void ownerAddsMember() throws Exception {
        call(get(project), bia).andExpect(status().isForbidden());

        json(post(project + "/members"), ana, "{\"email\": \"%s\"}".formatted(bia.email().toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(2));

        call(get(project), bia).andExpect(status().isOk());
        call(get(project + "/tasks"), bia).andExpect(status().isOk()).andExpect(jsonPath("$.totalItems").value(1));
        call(get(project + "/members"), bia)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(org.hamcrest.Matchers.contains("Ana", "Bia")))
                .andExpect(jsonPath("$[0].owner").value(true))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("só o dono gerencia membros; um membro comum recebe 403")
    void onlyOwnerManagesMembers() throws Exception {
        json(post(project + "/members"), ana, "{\"email\": \"%s\"}".formatted(bia.email())).andExpect(status().isOk());

        json(post(project + "/members"), bia, "{\"email\": \"%s\"}".formatted(carla.email())).andExpect(status().isForbidden());
        call(delete(project + "/members/" + ana.id()), bia).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("quem não é dono não descobre se um e-mail tem conta: 403 antes de procurar o e-mail")
    void strangerCannotProbeEmails() throws Exception {
        json(post(project + "/members"), carla, "{\"email\": \"ninguem@exemplo.com\"}").andExpect(status().isForbidden());
        json(post(project + "/members"), carla, "{\"email\": \"%s\"}".formatted(bia.email())).andExpect(status().isForbidden());

        json(post(project + "/members"), ana, "{\"email\": \"ninguem@exemplo.com\"}").andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("membro removido perde o acesso na hora; o dono não pode ser removido")
    void removeMember() throws Exception {
        json(post(project + "/members"), ana, "{\"email\": \"%s\"}".formatted(bia.email())).andExpect(status().isOk());

        call(delete(project + "/members/" + bia.id()), ana).andExpect(status().isNoContent());
        call(get(project), bia).andExpect(status().isForbidden());
        call(delete(project + "/members/" + ana.id()), ana).andExpect(status().isUnprocessableContent());
    }

    @Test
    @DisplayName("o papel ADMIN não dá acesso aos projetos dos outros: projeto é do time, não do sistema")
    void adminIsNotAMember() throws Exception {
        mvc.perform(get(project).with(TestUsers.admin(UUID.randomUUID().toString()))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/users").with(TestUsers.as(ana.id()))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/forbidden"));
        mvc.perform(get("/api/v1/admin/users").with(TestUsers.admin(UUID.randomUUID().toString()))).andExpect(status().isOk());
    }

    static Stream<String[]> projectRoutes() {
        return Stream.of(
                new String[] {"GET", "{project}"},
                new String[] {"PUT", "{project}"},
                new String[] {"DELETE", "{project}"},
                new String[] {"GET", "{project}/tasks"},
                new String[] {"POST", "{project}/tasks"},
                new String[] {"GET", "{project}/members"},
                new String[] {"POST", "{project}/members"},
                new String[] {"GET", "{project}/labels"},
                new String[] {"POST", "{project}/labels"},
                new String[] {"PATCH", "{task}/status"},
                new String[] {"PATCH", "{task}/assignee"},
                new String[] {"PUT", "{task}/labels"},
                new String[] {"GET", "{task}/comments"},
                new String[] {"POST", "{task}/comments"});
    }

    private MockHttpServletRequestBuilder request(String method, String route) {
        String uri = route.replace("{project}", project).replace("{task}", task);
        return MockMvcRequestBuilders.request(HttpMethod.valueOf(method), uri);
    }

    /** Corpos válidos, para a requisição chegar até a regra de permissão, e não parar na validação. */
    private static String validBody(String route) {
        if (route.endsWith("/members")) {
            return "{\"email\": \"alguem@exemplo.com\"}";
        }
        if (route.endsWith("/tasks")) {
            return "{\"title\": \"Tarefa nova\", \"priority\": \"LOW\"}";
        }
        if (route.endsWith("/status")) {
            return "{\"status\": \"IN_PROGRESS\"}";
        }
        if (route.endsWith("/assignee")) {
            return "{\"assigneeId\": \"%s\"}".formatted(UUID.randomUUID());
        }
        if (route.endsWith("/comments")) {
            return "{\"text\": \"Comentário\"}";
        }
        if (route.equals("{task}/labels")) {
            return "{\"labelIds\": []}";
        }
        if (route.endsWith("/labels")) {
            return "{\"name\": \"bug\", \"color\": \"#E11D48\"}";
        }
        return "{\"name\": \"Nome novo\"}";
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("projectRoutes")
    @DisplayName("sem token: 401 em toda rota do projeto")
    void withoutToken(String method, String route) throws Exception {
        json(request(method, route), null, validBody(route)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("projectRoutes")
    @DisplayName("com token de quem não é membro: 403 em toda rota do projeto, e nada muda")
    void strangerIsForbidden(String method, String route) throws Exception {
        json(request(method, route), carla, validBody(route))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/forbidden"));

        // Nada mudou: o projeto continua com o nome, o membro e a tarefa de antes.
        String detail = call(get(project), ana).andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(detail, "$.name")).isEqualTo("Lançamento");
        assertThat((Integer) JsonPath.read(detail, "$.members.length()")).isEqualTo(1);
        call(get(project + "/tasks"), ana).andExpect(jsonPath("$.totalItems").value(1));
    }
}
