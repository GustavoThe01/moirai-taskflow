package dev.gustavo.moirai.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ProjectJpaRepository extends JpaRepository<ProjectJpaEntity, String> {

    /**
     * O join com {@code members} serve só para filtrar. O {@code @EntityGraph} faz um segundo join, à parte, que traz
     * todos os membros de cada projeto na mesma consulta: sem ele, cada projeto da lista fazia uma consulta a mais (N+1).
     * Aqui não há paginação, então trazer a coleção junto é seguro.
     */
    @EntityGraph(attributePaths = "members")
    @Query("select distinct p from ProjectJpaEntity p join p.members m where m = :userId order by p.name")
    List<ProjectJpaEntity> findByMember(@Param("userId") String userId);
}
