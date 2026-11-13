package dev.gustavo.moirai.adapter.in.web;

import dev.gustavo.moirai.domain.BusinessRuleViolationException;
import dev.gustavo.moirai.domain.EmailAlreadyRegisteredException;
import dev.gustavo.moirai.domain.ForbiddenOperationException;
import dev.gustavo.moirai.domain.InvalidCredentialsException;
import dev.gustavo.moirai.domain.InvalidRefreshTokenException;
import dev.gustavo.moirai.domain.InvalidTaskTransitionException;
import dev.gustavo.moirai.domain.InvalidValueException;
import dev.gustavo.moirai.domain.NotProjectMemberException;
import dev.gustavo.moirai.domain.ProjectNotFoundException;
import dev.gustavo.moirai.domain.TaskNotFoundException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduz as exceções para o padrão ProblemDetail (RFC 9457). É aqui, e só aqui, que uma regra de negócio
 * vira um status HTTP: o domínio continua sem saber que a API existe.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({ProjectNotFoundException.class, TaskNotFoundException.class})
    ProblemDetail handleNotFound(RuntimeException exception) {
        return problem(HttpStatus.NOT_FOUND, "not-found", "Recurso não encontrado", exception.getMessage());
    }

    @ExceptionHandler({NotProjectMemberException.class, ForbiddenOperationException.class})
    ProblemDetail handleForbidden(RuntimeException exception) {
        return problem(HttpStatus.FORBIDDEN, "forbidden", "Operação não permitida", exception.getMessage());
    }

    @ExceptionHandler(InvalidTaskTransitionException.class)
    ProblemDetail handleInvalidTransition(InvalidTaskTransitionException exception) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "invalid-transition",
                "Transição de status inválida", exception.getMessage());
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    ProblemDetail handleBusinessRule(BusinessRuleViolationException exception) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "business-rule",
                "Regra de negócio violada", exception.getMessage());
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ProblemDetail handleEmailTaken(EmailAlreadyRegisteredException exception) {
        return problem(HttpStatus.CONFLICT, "email-taken", "E-mail já cadastrado", exception.getMessage());
    }

    /** Token aceito pela assinatura, mas que não identifica um usuário (o {@code sub} não é um id válido). */
    @ExceptionHandler(InvalidBearerTokenException.class)
    ProblemDetail handleTokenWithoutUser(InvalidBearerTokenException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "unauthorized", "Não autenticado", "O access token não identifica um usuário.");
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ProblemDetail handleInvalidRefreshToken(InvalidRefreshTokenException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "invalid-refresh-token", "Refresh token inválido", exception.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handleInvalidCredentials(InvalidCredentialsException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "invalid-credentials", "Credenciais inválidas", exception.getMessage());
    }

    @ExceptionHandler(InvalidValueException.class)
    ProblemDetail handleInvalidValue(InvalidValueException exception) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-value", "Valor inválido", exception.getMessage());
    }

    /** Erro inesperado: registra tudo no log e não revela nada do interior da aplicação a quem chamou. */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        LOG.error("Erro inesperado", exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente mais tarde.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "validation", "Dados inválidos",
                "Um ou mais campos não passaram na validação.");
        List<Map<String, String>> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", String.valueOf(error.getDefaultMessage())))
                .toList();
        problem.setProperty("errors", errors);
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    private static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("/problems/" + type));
        problem.setTitle(title);
        return problem;
    }
}
