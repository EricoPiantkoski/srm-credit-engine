package com.srm.creditengine.cambio.infrastructure.adapter.out.persistence;

import com.srm.creditengine.cambio.domain.MoedaRepository;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import org.springframework.stereotype.Component;

@Component
public class MoedaRepositoryAdapter implements MoedaRepository {

    private final MoedaJpaRepository jpaRepository;

    public MoedaRepositoryAdapter(MoedaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean exists(CodigoMoeda moeda) {
        return jpaRepository.existsByCodigo(moeda.codigo());
    }
}