package com.srm.creditengine.cambio.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MoedaJpaRepository extends JpaRepository<MoedaJpaEntity, String> {

    boolean existsByCodigo(String codigo);
}