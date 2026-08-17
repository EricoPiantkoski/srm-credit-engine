package com.srm.creditengine.precificacao.infrastructure.adapter.out.persistence;

import com.srm.creditengine.cambio.infrastructure.adapter.out.persistence.MoedaJpaRepository;
import com.srm.creditengine.precificacao.domain.MoedaCatalog;
import com.srm.creditengine.precificacao.domain.exception.UnknownCurrencyException;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import org.springframework.stereotype.Component;

@Component
public class MoedaCatalogAdapter implements MoedaCatalog {

    private final MoedaJpaRepository jpaRepository;

    public MoedaCatalogAdapter(MoedaJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public int scaleOf(CodigoMoeda moeda) {
        return jpaRepository.findById(moeda.codigo())
            .map(entity -> entity.getEscala())
            .orElseThrow(() -> new UnknownCurrencyException(moeda.codigo()));
    }
}