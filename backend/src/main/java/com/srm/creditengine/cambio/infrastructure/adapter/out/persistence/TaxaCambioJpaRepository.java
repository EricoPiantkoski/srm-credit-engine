package com.srm.creditengine.cambio.infrastructure.adapter.out.persistence;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaxaCambioJpaRepository extends JpaRepository<TaxaCambioJpaEntity, Long> {

    boolean existsByCodigoBaseAndCodigoCotacaoAndVigencia(String codigoBase, String codigoCotacao, Instant vigencia);

    Optional<TaxaCambioJpaEntity> findFirstByCodigoBaseAndCodigoCotacaoAndVigenciaLessThanEqualOrderByVigenciaDesc(
        String codigoBase, String codigoCotacao, Instant reference);
}