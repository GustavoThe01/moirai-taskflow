package dev.gustavo.moirai.adapter.in.web;

import dev.gustavo.moirai.application.port.in.LabelUseCases.CreateLabel;
import dev.gustavo.moirai.application.port.in.LabelUseCases.ListLabels;
import dev.gustavo.moirai.domain.Label;
import dev.gustavo.moirai.domain.ProjectId;
import dev.gustavo.moirai.domain.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/labels")
@Tag(name = "Etiquetas")
class LabelController {

    private static final String USER_HEADER = "X-User-Id";

    private final CreateLabel createLabel;
    private final ListLabels listLabels;

    LabelController(CreateLabel createLabel, ListLabels listLabels) {
        this.createLabel = createLabel;
        this.listLabels = listLabels;
    }

    record CreateLabelRequest(
            @Schema(example = "bug") @NotBlank @Size(max = 30) String name,
            @Schema(description = "Cor em hexadecimal", example = "#E11D48") @NotBlank @Pattern(regexp = "#[0-9A-Fa-f]{6}") String color) {
    }

    record LabelResponse(UUID id, String name, String color) {

        static LabelResponse from(Label label) {
            return new LabelResponse(label.id().value(), label.name(), label.color());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria uma etiqueta no projeto", description = "O nome é único no projeto, sem diferenciar maiúsculas.")
    @ApiResponse(responseCode = "201", description = "Etiqueta criada")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "403", ref = "Forbidden")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "422", ref = "UnprocessableContent")
    LabelResponse create(@RequestHeader(USER_HEADER) UUID user, @PathVariable UUID projectId,
                         @Valid @RequestBody CreateLabelRequest request) {
        return LabelResponse.from(createLabel.create(new ProjectId(projectId), request.name(), request.color(), new UserId(user)));
    }

    @GetMapping
    @Operation(summary = "Lista as etiquetas do projeto, em ordem de nome")
    @ApiResponse(responseCode = "200", description = "Etiquetas do projeto")
    @ApiResponse(responseCode = "403", ref = "Forbidden")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    List<LabelResponse> list(@RequestHeader(USER_HEADER) UUID user, @PathVariable UUID projectId) {
        return listLabels.list(new ProjectId(projectId), new UserId(user)).stream().map(LabelResponse::from).toList();
    }
}
