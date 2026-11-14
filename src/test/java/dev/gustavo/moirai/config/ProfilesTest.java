package dev.gustavo.moirai.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.gustavo.moirai.MoiraiApplication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.function.Consumer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Sobe a aplicação de verdade em cada profile, numa porta aleatória e com um banco temporário, e confere o que muda
 * entre eles. Sem {@code @SpringBootTest}: cada teste controla exatamente quais propriedades e profiles entram.
 */
@DisplayName("Profiles e configuração")
class ProfilesTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(new byte[32]);

    @TempDir
    Path folder;

    private String database() {
        return "jdbc:sqlite:" + folder.resolve("moirai.db").toString().replace('\\', '/') + "?foreign_keys=on";
    }

    /**
     * As propriedades entram como argumentos de linha de comando, a fonte de maior prioridade, como uma variável de
     * ambiente faria em produção. (As do {@code SpringApplicationBuilder.properties} têm a menor, e o
     * application.yml passaria por cima delas.)
     */
    private ConfigurableApplicationContext start(String profile, String... properties) {
        var builder = new SpringApplicationBuilder(MoiraiApplication.class);
        if (profile != null) {
            builder.profiles(profile);
        }
        var args = java.util.stream.Stream.concat(java.util.stream.Stream.of("server.port=0"), java.util.Arrays.stream(properties))
                .map(property -> "--" + property)
                .toArray(String[]::new);
        return builder.run(args);
    }

    private void withApp(String profile, Consumer<Integer> check, String... properties) {
        try (var context = start(profile, properties)) {
            check.accept(context.getEnvironment().getRequiredProperty("local.server.port", Integer.class));
        }
    }

    private static int status(int port, String path) {
        try {
            var response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                    HttpResponse.BodyHandlers.discarding());
            return response.statusCode();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    @DisplayName("sem profile informado, sobe em dev, com a documentação da API aberta")
    void defaultIsDev() {
        try (var context = start(null, "spring.datasource.url=" + database())) {
            assertThat(context.getEnvironment().getActiveProfiles()).isEmpty();
            assertThat(context.getEnvironment().getDefaultProfiles()).containsExactly("dev");
            int port = context.getEnvironment().getRequiredProperty("local.server.port", Integer.class);
            assertThat(status(port, "/v3/api-docs")).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("prod sem MOIRAI_JWT_SECRET não sobe, e a mensagem diz o que falta")
    void prodRequiresSecret() {
        assertThatThrownBy(() -> start("prod", "MOIRAI_DB_URL=" + database()).close())
                .hasStackTraceContaining("MOIRAI_JWT_SECRET");
    }

    @Test
    @DisplayName("prod sem MOIRAI_DB_URL não sobe")
    void prodRequiresDatabase() {
        assertThatThrownBy(() -> start("prod", "MOIRAI_JWT_SECRET=" + SECRET).close())
                .hasStackTraceContaining("MOIRAI_DB_URL");
    }

    @Test
    @DisplayName("prod com as variáveis sobe, esconde a documentação e mantém o health check")
    void prodStarts() {
        withApp("prod", port -> {
            assertThat(status(port, "/actuator/health")).isEqualTo(200);
            assertThat(status(port, "/v3/api-docs")).isIn(401, 404);
            assertThat(status(port, "/api/v1/projects")).isEqualTo(401);
        }, "MOIRAI_DB_URL=" + database(), "MOIRAI_JWT_SECRET=" + SECRET);
    }

    @Test
    @DisplayName("chave curta ou que não é Base64 não sobe, em qualquer profile")
    void weakSecret() {
        assertThatThrownBy(() -> start("dev", "spring.datasource.url=" + database(), "MOIRAI_JWT_SECRET=curta").close())
                .hasStackTraceContaining("32 bytes");
        String sixteenBytes = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> start("dev", "spring.datasource.url=" + database(), "MOIRAI_JWT_SECRET=" + sixteenBytes).close())
                .hasStackTraceContaining("32 bytes");
    }

    @Test
    @DisplayName("tempos de token absurdos não sobem")
    void invalidLifetimes() {
        assertThatThrownBy(() -> start("dev", "spring.datasource.url=" + database(),
                "moirai.security.access-token-lifetime=PT10H").close())
                .hasStackTraceContaining("access token");
    }

    @Test
    @DisplayName("o banco do profile vem da configuração, e não de um caminho fixo no código")
    void databaseFromConfiguration() throws Exception {
        withApp("prod", port -> assertThat(status(port, "/actuator/health")).isEqualTo(200),
                "MOIRAI_DB_URL=" + database(), "MOIRAI_JWT_SECRET=" + SECRET);

        assertThat(Files.exists(folder.resolve("moirai.db"))).isTrue();
    }
}
