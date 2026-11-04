package dev.gustavo.moirai.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface ProjectJpaRepository extends JpaRepository<ProjectJpaEntity, String> {
}
