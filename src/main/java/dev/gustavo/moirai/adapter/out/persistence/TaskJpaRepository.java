package dev.gustavo.moirai.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface TaskJpaRepository extends JpaRepository<TaskJpaEntity, String>, JpaSpecificationExecutor<TaskJpaEntity> {

    List<TaskJpaEntity> findByProjectIdOrderByCreatedAt(String projectId);
}
