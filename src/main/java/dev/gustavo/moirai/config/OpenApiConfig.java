package dev.gustavo.moirai.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documento OpenAPI da Moirai, servido em {@code /v3/api-docs} e navegável no Swagger UI.
 *
 * <p>Os erros seguem o ProblemDetail (RFC 9457). Em vez de repetir o formato em cada endpoint, defino as
 * respostas de erro uma vez em {@code components/responses}, e os controllers só apontam para elas.
 */
@Configuration
class OpenApiConfig {

    static final String BEARER = "bearerAuth";
    private static final String PROBLEM_JSON = "application/problem+json";

    @Bean
    OpenAPI moiraiOpenApi(@Value("${info.app.version:dev}") String version) {
        return new OpenAPI()
                .info(new Info()
                        .title("Moirai Taskflow API")
                        .version(version)
                        .description("""
                                Gestão de projetos e tarefas com fluxo de status controlado pelo domínio.

                                **Autenticação:** faça login em `POST /api/v1/auth/login` e envie o `accessToken` \
                                no cabeçalho `Authorization: Bearer <token>`. Ele vale 15 minutos; para um par novo, \
                                use o `refreshToken` em `POST /api/v1/auth/refresh`.

                                **Erros:** todas as respostas de erro usam ProblemDetail (RFC 9457), com \
                                `type`, `title`, `status`, `detail` e `instance`.""")
                        .contact(new Contact().name("Gustavo Osterno Thé").url("https://github.com/GustavoThe01"))
                        .license(new License().name("Código aberto para estudo e portfólio")))
                .addTagsItem(new Tag().name("Autenticação").description("Cadastro e login"))
                .addTagsItem(new Tag().name("Projetos").description("Criar, listar, renomear e excluir projetos"))
                .addTagsItem(new Tag().name("Tarefas").description("Criar, listar, atribuir e mover tarefas no fluxo"))
                .addTagsItem(new Tag().name("Etiquetas").description("Etiquetas do projeto, ligadas às tarefas (N:N)"))
                .addTagsItem(new Tag().name("Comentários").description("Conversa registrada em cada tarefa"))
                .addTagsItem(new Tag().name("Administração").description("Rotas do papel ADMIN"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))
                        .responses(problemResponses())
                        .addSchemas("ProblemDetail", problemSchema()));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Schema problemSchema() {
        Schema error = new Schema<>().type("object")
                .addProperty("field", new Schema<>().type("string").example("title"))
                .addProperty("message", new Schema<>().type("string").example("o tamanho deve ser entre 3 e 120"));
        return new Schema<>().type("object")
                .description("Erro no formato da RFC 9457")
                .addProperty("type", new Schema<>().type("string").format("uri-reference"))
                .addProperty("title", new Schema<>().type("string"))
                .addProperty("status", new Schema<>().type("integer").format("int32"))
                .addProperty("detail", new Schema<>().type("string"))
                .addProperty("instance", new Schema<>().type("string").format("uri-reference"))
                .addProperty("errors", new Schema<>().type("array").items(error)
                        .description("Só nos erros de validação: um item por campo"));
    }

    private static Map<String, ApiResponse> problemResponses() {
        Map<String, ApiResponse> responses = new LinkedHashMap<>();
        responses.put("BadRequest", problem("Dados inválidos: corpo mal formado, campo fora da regra ou parâmetro desconhecido",
                400, "validation", "Dados inválidos", "Um ou mais campos não passaram na validação."));
        responses.put("Unauthorized", problem("Sem access token válido, ou e-mail e senha que não conferem",
                401, "unauthorized", "Não autenticado", "Envie um access token válido no cabeçalho Authorization: Bearer <token>."));
        responses.put("Forbidden", problem("Quem chamou não é membro do projeto ou não tem o papel exigido",
                403, "forbidden", "Operação não permitida", "O usuário não é membro do projeto."));
        responses.put("NotFound", problem("Projeto ou tarefa não encontrado",
                404, "not-found", "Recurso não encontrado", "Tarefa 6a1f... não encontrada."));
        responses.put("UnprocessableContent", problem("A operação viola uma regra do domínio, como pular uma etapa do fluxo",
                422, "invalid-transition", "Transição de status inválida", "Uma tarefa em TODO não pode ir para DONE."));
        responses.put("Conflict", problem("Já existe uma conta com este e-mail",
                409, "email-taken", "E-mail já cadastrado", "Já existe uma conta com o e-mail ana@exemplo.com."));
        return responses;
    }

    private static ApiResponse problem(String description, int status, String type, String title, String detail) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "/problems/" + type);
        body.put("title", title);
        body.put("status", status);
        body.put("detail", detail);
        body.put("instance", "/api/v1/...");

        MediaType mediaType = new MediaType()
                .schema(new Schema<>().$ref("#/components/schemas/ProblemDetail"))
                .addExamples(type, new Example().value(body));
        return new ApiResponse().description(description).content(new Content().addMediaType(PROBLEM_JSON, mediaType));
    }
}
