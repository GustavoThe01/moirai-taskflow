package dev.gustavo.moirai.application.service;

import dev.gustavo.moirai.application.port.in.ProjectUseCases.CreateProject;
import dev.gustavo.moirai.application.port.in.ProjectUseCases.DeleteProject;
import dev.gustavo.moirai.application.port.in.ProjectUseCases.GetProject;
import dev.gustavo.moirai.application.port.in.ProjectUseCases.ListProjects;
import dev.gustavo.moirai.application.port.in.ProjectUseCases.RenameProject;
import dev.gustavo.moirai.application.port.out.ProjectRepository;
import dev.gustavo.moirai.domain.Project;
import dev.gustavo.moirai.domain.ProjectId;
import dev.gustavo.moirai.domain.ProjectNotFoundException;
import dev.gustavo.moirai.domain.UserId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orquestra os casos de uso de projetos. As regras continuam no domínio. */
@Service
class ProjectService implements CreateProject, GetProject, ListProjects, RenameProject, DeleteProject {

    private final ProjectRepository projects;

    ProjectService(ProjectRepository projects) {
        this.projects = projects;
    }

    @Override
    @Transactional
    public Project create(String name, UserId ownerId) {
        Project project = Project.create(name, ownerId);
        projects.save(project);
        return project;
    }

    @Override
    @Transactional(readOnly = true)
    public Project get(ProjectId id, UserId actor) {
        Project project = load(id);
        project.requireMember(actor);
        return project;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> listFor(UserId member) {
        return projects.findByMember(member);
    }

    @Override
    @Transactional
    public Project rename(ProjectId id, String newName, UserId actor) {
        Project project = load(id);
        project.rename(newName, actor);
        projects.save(project);
        return project;
    }

    @Override
    @Transactional
    public void delete(ProjectId id, UserId actor) {
        load(id).requireCanBeDeletedBy(actor);
        projects.delete(id);
    }

    private Project load(ProjectId id) {
        return projects.findById(id).orElseThrow(() -> new ProjectNotFoundException(id));
    }
}
