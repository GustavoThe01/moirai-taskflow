package dev.gustavo.moirai.adapter.in.web;

import dev.gustavo.moirai.application.port.in.SessionUseCases.EndSession;
import dev.gustavo.moirai.application.port.in.SessionUseCases.RefreshSession;
import dev.gustavo.moirai.application.port.in.SessionUseCases.Session;
import dev.gustavo.moirai.application.port.in.SessionUseCases.StartSession;
import dev.gustavo.moirai.application.port.in.UserUseCases.RegisterUser;
import dev.gustavo.moirai.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Cadastro, login, renovação e logout. São as únicas rotas da API que não exigem access token. */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação")
@SecurityRequirements
class AuthController {

    private final RegisterUser registerUser;
    private final StartSession startSession;
    private final RefreshSession refreshSession;
    private final EndSession endSession;
    private final Clock clock;

    AuthController(RegisterUser registerUser, StartSession startSession, RefreshSession refreshSession,
                   EndSession endSession, Clock clock) {
        this.registerUser = registerUser;
        this.startSession = startSession;
        this.refreshSession = refreshSession;
        this.endSession = endSession;
        this.clock = clock;
    }

    record RegisterRequest(
            @Schema(example = "ana@exemplo.com") @NotBlank @Size(max = 254) String email,
            @Schema(example = "Ana Lima") @NotBlank @Size(max = 100) String name,
            @Schema(example = "uma-senha-longa", minLength = 8) @NotBlank @Size(min = 8, max = 72) String password) {

        /** A senha nunca aparece no toString de um record, que pode acabar num log. */
        @Override
        public String toString() {
            return "RegisterRequest[email=%s, name=%s]".formatted(email, name);
        }
    }

    record LoginRequest(
            @Schema(example = "ana@exemplo.com") @NotBlank String email,
            @Schema(example = "uma-senha-longa") @NotBlank String password) {

        @Override
        public String toString() {
            return "LoginRequest[email=%s]".formatted(email);
        }
    }

    record RefreshRequest(@Schema(description = "O refresh token recebido no login ou na última renovação") @NotBlank String refreshToken) {

        @Override
        public String toString() {
            return "RefreshRequest[***]";
        }
    }

    /** O que a API devolve de um usuário: nunca o hash da senha. */
    record UserResponse(UUID id, String email, String name, String role, Instant createdAt) {

        static UserResponse from(User user) {
            return new UserResponse(user.id().value(), user.email(), user.name(), user.role().name(), user.createdAt());
        }
    }

    /** Resposta no formato do OAuth 2.0 (RFC 6749): {@code expiresIn} em segundos. */
    record TokenResponse(String accessToken, String tokenType, long expiresIn, String refreshToken,
                         long refreshExpiresIn, UserResponse user) {

        @Override
        public String toString() {
            return "TokenResponse[user=%s]".formatted(user);
        }
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma conta", description = "O e-mail é gravado em minúsculas e é único. A senha é guardada como hash BCrypt.")
    @ApiResponse(responseCode = "201", description = "Conta criada")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "409", ref = "Conflict")
    UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(registerUser.register(request.email(), request.name(), request.password()));
    }

    @PostMapping("/login")
    @Operation(summary = "Faz login e devolve o par de tokens",
            description = "O access token (JWT, 15 minutos) vai no cabeçalho Authorization: Bearer. "
                    + "O refresh token (7 dias, uso único) só serve para pedir um par novo em /auth/refresh.")
    @ApiResponse(responseCode = "200", description = "Credenciais corretas")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "401", ref = "Unauthorized")
    TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return toResponse(startSession.login(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Troca o refresh token por um par novo",
            description = "O refresh token usado deixa de valer. Se um token já usado aparecer de novo, a sessão inteira é encerrada.")
    @ApiResponse(responseCode = "200", description = "Par novo")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "401", ref = "Unauthorized")
    TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return toResponse(refreshSession.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Encerra a sessão", description = "Revoga o refresh token e os outros da mesma sessão. Sempre 204.")
    @ApiResponse(responseCode = "204", description = "Sessão encerrada")
    ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        endSession.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    private TokenResponse toResponse(Session session) {
        var now = clock.instant();
        return new TokenResponse(session.accessToken(), "Bearer",
                Duration.between(now, session.accessTokenExpiresAt()).toSeconds(),
                session.refreshToken(),
                Duration.between(now, session.refreshTokenExpiresAt()).toSeconds(),
                UserResponse.from(session.user()));
    }
}
