package dev.gustavo.moirai.adapter.in.web;

import dev.gustavo.moirai.application.port.in.UserUseCases.AuthenticateUser;
import dev.gustavo.moirai.application.port.in.UserUseCases.RegisterUser;
import dev.gustavo.moirai.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Cadastro e login. São as únicas rotas que nunca vão exigir autenticação. */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação")
class AuthController {

    private final RegisterUser registerUser;
    private final AuthenticateUser authenticateUser;

    AuthController(RegisterUser registerUser, AuthenticateUser authenticateUser) {
        this.registerUser = registerUser;
        this.authenticateUser = authenticateUser;
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

    /** O que a API devolve de um usuário: nunca o hash da senha. */
    record UserResponse(UUID id, String email, String name, Instant createdAt) {

        static UserResponse from(User user) {
            return new UserResponse(user.id().value(), user.email(), user.name(), user.createdAt());
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
    @Operation(summary = "Confere e-mail e senha",
            description = "Por enquanto devolve os dados do usuário. No Dia 12, devolve os tokens JWT.")
    @ApiResponse(responseCode = "200", description = "Credenciais corretas")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "401", ref = "Unauthorized")
    UserResponse login(@Valid @RequestBody LoginRequest request) {
        return UserResponse.from(authenticateUser.authenticate(request.email(), request.password()));
    }
}
