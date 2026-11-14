package dev.gustavo.moirai.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.gustavo.moirai.application.port.out.ProjectRepository;
import dev.gustavo.moirai.application.port.out.UserRepository;
import dev.gustavo.moirai.domain.BusinessRuleViolationException;
import dev.gustavo.moirai.domain.ForbiddenOperationException;
import dev.gustavo.moirai.domain.NotProjectMemberException;
import dev.gustavo.moirai.domain.Project;
import dev.gustavo.moirai.domain.User;
import dev.gustavo.moirai.domain.UserNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MembershipService")
class MembershipServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");

    private final ProjectRepository projects = mock(ProjectRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final MembershipService service = new MembershipService(projects, users);
    private final User ana = User.register("ana@exemplo.com", "Ana", "hash", NOW);
    private final User bia = User.register("bia@exemplo.com", "Bia", "hash", NOW);
    private final Project project = Project.create("Lançamento", ana.id());

    MembershipServiceTest() {
        when(projects.findById(project.id())).thenReturn(Optional.of(project));
        when(users.findByEmail("bia@exemplo.com")).thenReturn(Optional.of(bia));
    }

    @Test
    @DisplayName("o dono adiciona pelo e-mail, sem diferenciar maiúsculas")
    void ownerAdds() {
        service.addMember(project.id(), " BIA@exemplo.com ", ana.id());

        assertThat(project.isMember(bia.id())).isTrue();
        verify(projects).save(project);
    }

    @Test
    @DisplayName("quem não é dono recebe 403 antes de o e-mail ser procurado")
    void permissionBeforeLookup() {
        assertThatThrownBy(() -> service.addMember(project.id(), "bia@exemplo.com", bia.id()))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(users, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("e-mail sem conta dá UserNotFoundException")
    void unknownEmail() {
        when(users.findByEmail("ninguem@exemplo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addMember(project.id(), "ninguem@exemplo.com", ana.id()))
                .isInstanceOf(UserNotFoundException.class);
        verify(projects, never()).save(any());
    }

    @Test
    @DisplayName("o dono não pode ser removido")
    void ownerStays() {
        assertThatThrownBy(() -> service.removeMember(project.id(), ana.id(), ana.id()))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("listar membros exige ser membro, e vem em ordem de nome")
    void listMembers() {
        project.addMember(bia.id(), ana.id());
        when(users.findAllById(project.members())).thenReturn(List.of(bia, ana));

        assertThat(service.listMembers(project.id(), bia.id())).extracting(User::name).containsExactly("Ana", "Bia");
        assertThatThrownBy(() -> service.listMembers(project.id(), User.register("x@exemplo.com", "X", "h", NOW).id()))
                .isInstanceOf(NotProjectMemberException.class);
    }
}
