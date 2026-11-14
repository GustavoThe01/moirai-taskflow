package dev.gustavo.moirai.application.service;

import dev.gustavo.moirai.application.port.in.ProjectUseCases.ManageMembers;
import dev.gustavo.moirai.application.port.out.ProjectRepository;
import dev.gustavo.moirai.application.port.out.UserRepository;
import dev.gustavo.moirai.domain.Project;
import dev.gustavo.moirai.domain.ProjectId;
import dev.gustavo.moirai.domain.ProjectNotFoundException;
import dev.gustavo.moirai.domain.User;
import dev.gustavo.moirai.domain.UserId;
import dev.gustavo.moirai.domain.UserNotFoundException;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Membros do projeto. A permissão (só o dono adiciona e remove, só membros veem) continua no domínio, em
 * {@link Project}: o caso de uso só busca os dados e chama a regra.
 */
@Service
class MembershipService implements ManageMembers {

    private final ProjectRepository projects;
    private final UserRepository users;

    MembershipService(ProjectRepository projects, UserRepository users) {
        this.projects = projects;
        this.users = users;
    }

    @Override
    @Transactional
    public Project addMember(ProjectId id, String email, UserId actor) {
        Project project = load(id);
        // A permissão vem antes da busca do e-mail: quem não é dono não descobre se um e-mail tem conta.
        project.requireOwner(actor, "adicionar membros");
        User user = findByEmail(email);
        project.addMember(user.id(), actor);
        projects.save(project);
        return project;
    }

    @Override
    @Transactional
    public Project removeMember(ProjectId id, UserId member, UserId actor) {
        Project project = load(id);
        project.removeMember(member, actor);
        projects.save(project);
        return project;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> listMembers(ProjectId id, UserId actor) {
        Project project = load(id);
        project.requireMember(actor);
        return users.findAllById(project.members()).stream()
                .sorted(Comparator.comparing(User::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private User findByEmail(String email) {
        String normalized = User.normalizeEmail(email);
        return users.findByEmail(normalized).orElseThrow(() -> new UserNotFoundException(normalized));
    }

    private Project load(ProjectId id) {
        return projects.findById(id).orElseThrow(() -> new ProjectNotFoundException(id));
    }
}
