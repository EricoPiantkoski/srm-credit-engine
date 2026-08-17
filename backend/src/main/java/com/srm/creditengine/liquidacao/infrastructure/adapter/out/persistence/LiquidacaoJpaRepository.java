package com.srm.creditengine.liquidacao.infrastructure.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LiquidacaoJpaRepository extends JpaRepository<LiquidacaoJpaEntity, Long> {

    boolean existsByChaveIdempotencia(String chaveIdempotencia);

    @EntityGraph(attributePaths = "itens")
    Optional<LiquidacaoJpaEntity> findWithItensById(Long id);
}