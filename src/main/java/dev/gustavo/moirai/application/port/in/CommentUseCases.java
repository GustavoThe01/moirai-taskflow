package dev.gustavo.moirai.application.port.in;

import dev.gustavo.moirai.domain.Comment;
import dev.gustavo.moirai.domain.TaskId;
import dev.gustavo.moirai.domain.UserId;
import java.util.List;

/** Portas de entrada dos comentários de uma tarefa. */
public final class CommentUseCases {

    private CommentUseCases() {
    }

    public interface AddComment {
        Comment add(TaskId taskId, String text, UserId author);
    }

    public interface ListComments {
        /** Comentários da tarefa, do mais antigo para o mais recente. Só membros podem ver. */
        List<Comment> list(TaskId taskId, UserId actor);
    }
}
