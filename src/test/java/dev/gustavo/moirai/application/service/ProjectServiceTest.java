package dev.gustavo.moirai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.gustavo.moirai.application.port.out.ProjectRepository;
import dev.gustavo.moirai.domain.BusinessRuleViolationException;
import dev.gustavo.moirai.domain.NotProjectMemberException;
import dev.gustavo.moirai.domain.Project;
import dev.gustavo.moirai.domain.ProjectId;
import dev.gustavo.moirai.domain.ProjectNotFoundException;
import dev.gustavo.moirai.domain.UserId;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Caso de uso testado sem Spring e sem banco: a porta de saída é substituída por um mock. */
@DisplayName("ProjectService")
class ProjectServiceTest {

    private final ProjectRepository repository = mock(ProjectRepository.class);
    private final ProjectService service = new ProjectService(repository);
    private final UserId owner = UserId.newId();
    private final UserId stranger = UserId.newId();

    private Project existing() {
        Project project = Project.create("Moirai", owner);
        when(repository.findById(project.id())).thenReturn(Optional.of(project));
        return project;
    }

    @Test
    @DisplayName("criar salva o projeto")
    void createSaves() {
        Project project = service.create("Moirai", owner);

        verify(repository).save(project);
        assertThat(project.isMember(owner)).isTrue();
    }

    @Test
    @DisplayName("quem não é membro não enxerga o projeto")
    void getRequiresMember() {
        Project project = existing();

        assertThatThrownBy(() -> service.get(project.id(), stranger)).isInstanceOf(NotProjectMemberException.class);
    }

    @Test
    @DisplayName("id inexistente lança ProjectNotFoundException")
    void missingProject() {
        ProjectId id = ProjectId.newId();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id, owner)).isInstanceOf(ProjectNotFoundException.class);
    }

    @Test
    @DisplayName("renomear sem ser dono não salva nada")
    void renameByStrangerSavesNothing() {
        Project project = existing();

        assertThatThrownBy(() -> service.rename(project.id(), "Novo nome", stranger))
                .isInstanceOf(BusinessRuleViolationException.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("excluir sem ser dono não apaga nada")
    void deleteByStrangerDeletesNothing() {
        Project project = existing();

        assertThatThrownBy(() -> service.delete(project.id(), stranger))
                .isInstanceOf(BusinessRuleViolationException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("o dono consegue excluir")
    void ownerDeletes() {
        Project project = existing();

        service.delete(project.id(), owner);

        verify(repository).delete(project.id());
    }
}
