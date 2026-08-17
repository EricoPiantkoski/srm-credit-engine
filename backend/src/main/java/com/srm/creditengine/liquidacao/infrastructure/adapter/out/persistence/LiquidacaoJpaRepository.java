package com.srm.creditengine.liquidacao.infrastructure.adapter.out.persistence;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LiquidacaoJpaRepository extends JpaRepository<LiquidacaoJpaEntity, Long> {

    boolean existsByChaveIdempotencia(String chaveIdempotencia);

    @EntityGraph(attributePaths = "itens")
    Optional<LiquidacaoJpaEntity> findWithItensById(Long id);

    @EntityGraph(attributePaths = "itens")
    Page<LiquidacaoJpaEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
