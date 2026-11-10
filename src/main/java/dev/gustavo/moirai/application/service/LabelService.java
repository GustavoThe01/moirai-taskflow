package dev.gustavo.moirai.application.service;

import dev.gustavo.moirai.application.port.in.LabelUseCases.CreateLabel;
import dev.gustavo.moirai.application.port.in.LabelUseCases.ListLabels;
import dev.gustavo.moirai.application.port.out.LabelRepository;
import dev.gustavo.moirai.application.port.out.ProjectRepository;
import dev.gustavo.moirai.domain.BusinessRuleViolationException;
import dev.gustavo.moirai.domain.Label;
import dev.gustavo.moirai.domain.Project;
import dev.gustavo.moirai.domain.ProjectId;
import dev.gustavo.moirai.domain.ProjectNotFoundException;
import dev.gustavo.moirai.domain.UserId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class LabelService implements CreateLabel, ListLabels {

    private final ProjectRepository projects;
    private final LabelRepository labels;

    LabelService(ProjectRepository projects, LabelRepository labels) {
        this.projects = projects;
        this.labels = labels;
    }

    @Override
    @Transactional
    public Label create(ProjectId projectId, String name, String color, UserId actor) {
        Label label = Label.create(load(projectId), actor, name, color);
        // Mensagem clara para o caso comum. O índice único do banco continua sendo a garantia final.
        boolean taken = labels.findByProject(projectId).stream()
                .anyMatch(existing -> existing.name().equalsIgnoreCase(label.name()));
        if (taken) {
            throw new BusinessRuleViolationException("O projeto já tem a etiqueta \"%s\".".formatted(label.name()));
        }
        labels.save(label);
        return label;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Label> list(ProjectId projectId, UserId actor) {
        load(projectId).requireMember(actor);
        return labels.findByProject(projectId);
    }

    private Project load(ProjectId id) {
        return projects.findById(id).orElseThrow(() -> new ProjectNotFoundException(id));
    }
}
