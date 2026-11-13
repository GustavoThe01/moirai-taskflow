package dev.gustavo.moirai.adapter.in.web;

import dev.gustavo.moirai.application.port.in.CommentUseCases.AddComment;
import dev.gustavo.moirai.application.port.in.CommentUseCases.ListComments;
import dev.gustavo.moirai.domain.Comment;
import dev.gustavo.moirai.domain.TaskId;
import dev.gustavo.moirai.domain.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks/{taskId}/comments")
@Tag(name = "Comentários")
class CommentController {


    private final AddComment addComment;
    private final ListComments listComments;

    CommentController(AddComment addComment, ListComments listComments) {
        this.addComment = addComment;
        this.listComments = listComments;
    }

    record AddCommentRequest(@Schema(example = "Testei no ambiente de homologação e funcionou.") @NotBlank @Size(max = 2000) String text) {
    }

    record CommentResponse(UUID id, UUID authorId, String text, Instant createdAt) {

        static CommentResponse from(Comment comment) {
            return new CommentResponse(comment.id().value(), comment.author().value(), comment.text(), comment.createdAt());
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Comenta numa tarefa", description = "Só membros do projeto comentam. Tarefa concluída também aceita comentário.")
    @ApiResponse(responseCode = "201", description = "Comentário registrado")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "403", ref = "Forbidden")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    CommentResponse add(@CurrentUser UserId user, @PathVariable UUID taskId,
                        @Valid @RequestBody AddCommentRequest request) {
        return CommentResponse.from(addComment.add(new TaskId(taskId), request.text(), user));
    }

    @GetMapping
    @Operation(summary = "Lista os comentários da tarefa, do mais antigo para o mais recente")
    @ApiResponse(responseCode = "200", description = "Comentários da tarefa")
    @ApiResponse(responseCode = "403", ref = "Forbidden")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    List<CommentResponse> list(@CurrentUser UserId user, @PathVariable UUID taskId) {
        return listComments.list(new TaskId(taskId), user).stream().map(CommentResponse::from).toList();
    }
}
