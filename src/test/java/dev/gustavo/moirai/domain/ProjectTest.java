package dev.gustavo.moirai.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Project")
class ProjectTest {

    private final UserId owner = UserId.newId();
    private final UserId ana = UserId.newId();

    @Test
    @DisplayName("o dono já nasce como membro")
    void ownerIsMember() {
        Project project = Project.create("Portfólio", owner);
        assertThat(project.isMember(owner)).isTrue();
        assertThat(project.members()).containsExactly(owner);
    }

    @Test
    @DisplayName("o dono adiciona membros")
    void ownerAddsMember() {
        Project project = Project.create("Portfólio", owner);
        project.addMember(ana, owner);
        assertThat(project.isMember(ana)).isTrue();
    }

    @Test
    @DisplayName("quem não é dono não adiciona membros")
    void nonOwnerCannotAddMember() {
        Project project = Project.create("Portfólio", owner);
        project.addMember(ana, owner);

        assertThatThrownBy(() -> project.addMember(UserId.newId(), ana))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("Só o dono");
    }

    @Test
    @DisplayName("o dono não pode ser removido")
    void ownerCannotBeRemoved() {
        Project project = Project.create("Portfólio", owner);
        assertThatThrownBy(() -> project.removeMember(owner, owner))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("requireMember lança exceção para quem não é membro")
    void requireMemberFails() {
        Project project = Project.create("Portfólio", owner);
        assertThatThrownBy(() -> project.requireMember(ana)).isInstanceOf(NotProjectMemberException.class);
    }

    @Test
    @DisplayName("rejeita nome com menos de 3 caracteres")
    void rejectsShortName() {
        assertThatThrownBy(() -> Project.create(" ab ", owner)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    @DisplayName("a lista de membros não pode ser alterada por fora")
    void membersAreUnmodifiable() {
        Project project = Project.create("Portfólio", owner);
        assertThatThrownBy(() -> project.members().add(ana)).isInstanceOf(UnsupportedOperationException.class);
    }
}
