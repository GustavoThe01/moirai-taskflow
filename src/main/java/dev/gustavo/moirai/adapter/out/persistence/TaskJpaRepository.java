package dev.gustavo.moirai.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface TaskJpaRepository extends JpaRepository<TaskJpaEntity, String> {

    List<TaskJpaEntity> findByProjectIdOrderByCreatedAt(String projectId);
}
