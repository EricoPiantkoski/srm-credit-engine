package com.srm.creditengine.precificacao.infrastructure.adapter.out.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecebivelJpaRepository extends JpaRepository<RecebivelJpaEntity, Long> {

    @Modifying
    @Query("update RecebivelJpaEntity r set r.version = r.version + 1, r.status = 'LIQUIDADO' "
        + "where r.id = :id and r.version = :expectedVersion and r.status = 'DISPONIVEL'")
    int marcarLiquidado(@Param("id") Long id, @Param("expectedVersion") Long expectedVersion);

    boolean existsByReferenciaExterna(String referenciaExterna);

    Optional<RecebivelJpaEntity> findByReferenciaExterna(String referenciaExterna);

    Page<RecebivelJpaEntity> findByCedente(String cedente, Pageable pageable);

    Page<RecebivelJpaEntity> findByCodigoMoeda(String codigoMoeda, Pageable pageable);

    Page<RecebivelJpaEntity> findByCodigoTipo(String codigoTipo, Pageable pageable);

    Page<RecebivelJpaEntity> findByCedenteAndCodigoMoeda(String cedente, String codigoMoeda, Pageable pageable);

    Page<RecebivelJpaEntity> findByCedenteAndCodigoTipo(String cedente, String codigoTipo, Pageable pageable);

    Page<RecebivelJpaEntity> findByCodigoMoedaAndCodigoTipo(String codigoMoeda, String codigoTipo, Pageable pageable);

    Page<RecebivelJpaEntity> findByCedenteAndCodigoMoedaAndCodigoTipo(
        String cedente, String codigoMoeda, String codigoTipo, Pageable pageable);
}