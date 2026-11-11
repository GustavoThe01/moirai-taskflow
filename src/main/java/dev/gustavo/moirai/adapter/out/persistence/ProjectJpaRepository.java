package dev.gustavo.moirai.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * Exclusão lógica: marca a data em vez de apagar a linha.
     *
     * <p>SQL nativo de propósito. Em JPQL, o Hibernate acrescenta o {@code @SQLRestriction} com um apelido de tabela,
     * e o SQLite não aceita apelido num UPDATE.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE project SET deleted_at = :now WHERE id = :id AND deleted_at IS NULL", nativeQuery = true)
    int softDelete(@Param("id") String id, @Param("now") Instant now);
}
