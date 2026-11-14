package dev.gustavo.moirai.adapter.in.web;

import dev.gustavo.moirai.application.port.in.SessionUseCases.ListUsers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rotas de administração. O papel é conferido por {@code @PreAuthorize}, no próprio método: quem lê o código vê a regra
 * junto da rota, e uma rota nova sem a anotação aparece no teste de segurança, que exige 403 para quem não é ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Administração")
class AdminController {

    private final ListUsers listUsers;

    AdminController(ListUsers listUsers) {
        this.listUsers = listUsers;
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lista os usuários", description = "Só para o papel ADMIN.")
    @ApiResponse(responseCode = "200", description = "Usuários, na ordem de cadastro")
    @ApiResponse(responseCode = "401", ref = "Unauthorized")
    @ApiResponse(responseCode = "403", ref = "Forbidden")
    List<AuthController.UserResponse> users() {
        return listUsers.listAll().stream().map(AuthController.UserResponse::from).toList();
    }
}
