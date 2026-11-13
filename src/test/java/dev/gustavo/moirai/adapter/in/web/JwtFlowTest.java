package dev.gustavo.moirai.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** O ciclo do JWT de verdade: login, access token nas rotas, renovação com rotação, reutilização, logout e papéis. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("JWT de ponta a ponta")
class JwtFlowTest {

    private static final String PASSWORD = "senha-bem-longa";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private JdbcTemplate jdbc;

    private ResultActions postJson(String path, String body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    /** Cadastra (se ainda não existir) e faz login. Devolve o corpo da resposta do login. */
    private String loginAs(String email) throws Exception {
        postJson("/api/v1/auth/register", "{\"email\": \"%s\", \"name\": \"Teste\", \"password\": \"%s\"}".formatted(email, PASSWORD));
        return postJson("/api/v1/auth/login", "{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, PASSWORD))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String newUser() throws Exception {
        return loginAs("jwt." + UUID.randomUUID() + "@exemplo.com");
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return postJson("/api/v1/auth/refresh", "{\"refreshToken\": \"%s\"}".formatted(refreshToken));
    }

    private ResultActions projectsWith(String accessToken) throws Exception {
        return mvc.perform(get("/api/v1/projects").header("Authorization", "Bearer " + accessToken));
    }

    private static String read(String json, String path) {
        return JsonPath.read(json, path).toString();
    }

    @Test
    @DisplayName("login devolve um JWT de 15 minutos e um refresh token de 7 dias; o JWT abre as rotas")
    void loginGivesTokens() throws Exception {
        String login = newUser();

        assertThat(read(login, "$.tokenType")).isEqualTo("Bearer");
        assertThat(Long.parseLong(read(login, "$.expiresIn"))).isBetween(890L, 900L);
        assertThat(Long.parseLong(read(login, "$.refreshExpiresIn"))).isBetween(604_790L, 604_800L);

        String payload = new String(Base64.getUrlDecoder().decode(read(login, "$.accessToken").split("\\.")[1]), StandardCharsets.UTF_8);
        assertThat(payload).contains("\"sub\":\"" + read(login, "$.user.id") + "\"", "\"roles\":[\"USER\"]", "\"iss\":\"moirai-taskflow\"");

        projectsWith(read(login, "$.accessToken")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("sem token, com token adulterado ou de outro emissor: 401 com WWW-Authenticate")
    void rejectsBadTokens() throws Exception {
        String access = read(newUser(), "$.accessToken");
        String[] parts = access.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8).replace("\"USER\"", "\"ADMIN\"");
        String tampered = parts[0] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + parts[2];

        mvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Bearer")))
                .andExpect(jsonPath("$.type").value("/problems/unauthorized"));
        projectsWith(tampered).andExpect(status().isUnauthorized());
        projectsWith(handMade("outro-emissor", Instant.now().plusSeconds(600))).andExpect(status().isUnauthorized());
        projectsWith(handMade("moirai-taskflow", Instant.now().plusSeconds(600))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("token vencido dá 401")
    void rejectsExpiredToken() throws Exception {
        projectsWith(handMade("moirai-taskflow", Instant.now().minusSeconds(120)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.containsString("expired")));
    }

    @Test
    @DisplayName("refresh troca o par e o token usado deixa de valer; reutilizá-lo derruba a sessão inteira")
    void refreshRotatesAndDetectsReuse() throws Exception {
        String first = read(newUser(), "$.refreshToken");

        String second = read(refresh(first).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.refreshToken");
        assertThat(second).isNotEqualTo(first);

        // O primeiro token voltou: alguém o copiou. A resposta é 401, e o segundo também deixa de valer.
        refresh(first).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.type").value("/problems/invalid-refresh-token"));
        refresh(second).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("logout revoga o refresh token; o banco guarda só o hash dele")
    void logoutRevokes() throws Exception {
        String login = newUser();
        String refreshToken = read(login, "$.refreshToken");

        Integer stored = jdbc.queryForObject("SELECT count(*) FROM refresh_token WHERE token_hash = ?", Integer.class, refreshToken);
        assertThat(stored).isZero();
        Integer forUser = jdbc.queryForObject("SELECT count(*) FROM refresh_token WHERE user_id = ?", Integer.class, read(login, "$.user.id"));
        assertThat(forUser).isEqualTo(1);

        postJson("/api/v1/auth/logout", "{\"refreshToken\": \"%s\"}".formatted(refreshToken)).andExpect(status().isNoContent());
        refresh(refreshToken).andExpect(status().isUnauthorized());
        postJson("/api/v1/auth/logout", "{\"refreshToken\": \"%s\"}".formatted(refreshToken)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("rota de administração: 403 para USER, 200 para ADMIN")
    void adminRoute() throws Exception {
        String user = read(newUser(), "$.accessToken");
        String admin = loginAs("admin.moirai@exemplo.com");
        assertThat(read(admin, "$.user.role")).isEqualTo("ADMIN");

        mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + user))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/forbidden"));
        mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + read(admin, "$.accessToken")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", org.hamcrest.Matchers.hasItem("admin.moirai@exemplo.com")))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    /** Token assinado com a chave da aplicação, mas com emissor e validade escolhidos pelo teste. */
    private String handMade(String issuer, Instant expiresAt) {
        Instant issuedAt = expiresAt.minusSeconds(900);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .notBefore(issuedAt)
                .expiresAt(expiresAt)
                .claim("roles", List.of("USER"))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
