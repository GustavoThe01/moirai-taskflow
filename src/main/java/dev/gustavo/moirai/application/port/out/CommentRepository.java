package dev.gustavo.moirai.application.port.out;

import dev.gustavo.moirai.domain.Comment;
import dev.gustavo.moirai.domain.TaskId;
import java.util.List;

/** Porta de saída dos comentários. Comentário só é gravado e lido: não existe editar. */
public interface CommentRepository {

    void save(Comment comment);

    /** Comentários da tarefa, do mais antigo para o mais recente. */
    List<Comment> findByTask(TaskId taskId);
}
