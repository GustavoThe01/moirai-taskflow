package dev.gustavo.moirai.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProjectJpaRepository extends JpaRepository<ProjectJpaEntity, String> {

    @Query("select p from ProjectJpaEntity p join p.members m where m = :userId order by p.name")
    List<ProjectJpaEntity> findByMember(@Param("userId") String userId);
}
