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
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
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

    static final String USER_HEADER = "X-User-Id";
    private static final String PROBLEM_JSON = "application/problem+json";

    @Bean
    OpenAPI moiraiOpenApi(@Value("${info.app.version:dev}") String version) {
        return new OpenAPI()
                .info(new Info()
                        .title("Moirai Taskflow API")
                        .version(version)
                        .description("""
                                Gestão de projetos e tarefas com fluxo de status controlado pelo domínio.

                                **Autenticação provisória:** até o Dia 12 do roadmap, quem faz a requisição é \
                                identificado pelo cabeçalho `X-User-Id` (um UUID). No Dia 12 ele dá lugar ao JWT.

                                **Erros:** todas as respostas de erro usam ProblemDetail (RFC 9457), com \
                                `type`, `title`, `status`, `detail` e `instance`.""")
                        .contact(new Contact().name("Gustavo Osterno Thé").url("https://github.com/GustavoThe01"))
                        .license(new License().name("Código aberto para estudo e portfólio")))
                .addTagsItem(new Tag().name("Autenticação").description("Cadastro e login"))
                .addTagsItem(new Tag().name("Projetos").description("Criar, listar, renomear e excluir projetos"))
                .addTagsItem(new Tag().name("Tarefas").description("Criar, listar, atribuir e mover tarefas no fluxo"))
                .addTagsItem(new Tag().name("Etiquetas").description("Etiquetas do projeto, ligadas às tarefas (N:N)"))
                .addTagsItem(new Tag().name("Comentários").description("Conversa registrada em cada tarefa"))
                .components(new Components().responses(problemResponses()).addSchemas("ProblemDetail", problemSchema()));
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

    /** Descreve o cabeçalho de usuário em todas as operações que o usam, sem repetir o texto em cada controller. */
    @Bean
    OpenApiCustomizer userHeaderCustomizer() {
        return openApi -> openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
            if (operation.getParameters() == null) {
                return;
            }
            operation.getParameters().stream()
                    .filter(parameter -> USER_HEADER.equals(parameter.getName()) && "header".equals(parameter.getIn()))
                    .forEach(OpenApiConfig::describeUserHeader);
        }));
    }

    private static void describeUserHeader(Parameter parameter) {
        parameter.description("Id do usuário que faz a requisição. Provisório: o JWT substitui este cabeçalho no Dia 12.")
                .example("3f6c1a52-7c1e-4b7e-9a51-0d9b2f1e8c11");
    }

    private static Map<String, ApiResponse> problemResponses() {
        Map<String, ApiResponse> responses = new LinkedHashMap<>();
        responses.put("BadRequest", problem("Dados inválidos: corpo mal formado, campo fora da regra ou parâmetro desconhecido",
                400, "validation", "Dados inválidos", "Um ou mais campos não passaram na validação."));
        responses.put("Unauthorized", problem("E-mail ou senha não conferem (a mensagem não diz qual dos dois)",
                401, "invalid-credentials", "Credenciais inválidas", "E-mail ou senha inválidos."));
        responses.put("Forbidden", problem("Quem chamou não é membro do projeto ou não pode fazer esta operação",
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
