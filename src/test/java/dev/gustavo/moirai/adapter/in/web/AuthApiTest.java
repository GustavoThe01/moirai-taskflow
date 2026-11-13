package dev.gustavo.moirai.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.ResultActions;

/** Cadastro e login de ponta a ponta: Spring Security, BCrypt de verdade e SQLite com as migrations. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("API de autenticação")
class AuthApiTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    // O banco de teste é compartilhado entre as classes; um e-mail novo por teste evita colisão.
    private final String email = "ana." + UUID.randomUUID() + "@exemplo.com";

    private ResultActions postJson(String path, String body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions register(String mail, String password) throws Exception {
        return postJson("/api/v1/auth/register",
                "{\"email\": \"%s\", \"name\": \"Ana Lima\", \"password\": \"%s\"}".formatted(mail, password));
    }

    private ResultActions login(String mail, String password) throws Exception {
        return postJson("/api/v1/auth/login", "{\"email\": \"%s\", \"password\": \"%s\"}".formatted(mail, password));
    }

    @Test
    @DisplayName("cadastro devolve 201 sem o hash, e o banco guarda um hash BCrypt, nunca a senha")
    void registerStoresBcryptHash() throws Exception {
        register(email.toUpperCase(), "senha-bem-longa")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        String hash = jdbc.queryForObject("SELECT password_hash FROM app_user WHERE email = ?", String.class, email);
        assertThat(hash).startsWith("$2a$10$").doesNotContain("senha-bem-longa");
    }

    @Test
    @DisplayName("o mesmo e-mail duas vezes dá 409")
    void duplicateEmail() throws Exception {
        register(email, "senha-bem-longa").andExpect(status().isCreated());

        register(email, "outra-senha-longa")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("/problems/email-taken"));
    }

    @Test
    @DisplayName("senha curta ou e-mail inválido dão 400")
    void validation() throws Exception {
        register(email, "curta").andExpect(status().isBadRequest());
        register("sem-arroba", "senha-bem-longa").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("login com a senha certa dá 200; com a errada ou e-mail desconhecido, o mesmo 401")
    void login() throws Exception {
        register(email, "senha-bem-longa").andExpect(status().isCreated());

        login(email, "senha-bem-longa").andExpect(status().isOk()).andExpect(jsonPath("$.user.email").value(email));

        String wrongPassword = login(email, "senha-errada!!").andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String unknownEmail = login("ninguem." + UUID.randomUUID() + "@exemplo.com", "senha-bem-longa")
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertThat(wrongPassword).contains("E-mail ou senha inválidos.");
        assertThat(unknownEmail).contains("E-mail ou senha inválidos.");
    }

    @Test
    @DisplayName("sem sessão: nenhuma resposta cria cookie, e POST funciona sem token CSRF")
    void stateless() throws Exception {
        register(email, "senha-bem-longa")
                .andExpect(status().isCreated())
                .andExpect(result -> assertThat(result.getResponse().getHeader("Set-Cookie")).isNull());
        login(email, "senha-bem-longa")
                .andExpect(result -> assertThat(result.getRequest().getSession(false)).isNull());
    }
}
