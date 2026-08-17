package com.srm.creditengine.precificacao.infrastructure.adapter.out.persistence;

import com.srm.creditengine.precificacao.domain.TipoRecebivel;
import com.srm.creditengine.precificacao.domain.TipoRecebivelRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TipoRecebivelRepositoryAdapter implements TipoRecebivelRepository {

    private final TipoRecebivelJpaRepository jpaRepository;

    public TipoRecebivelRepositoryAdapter(TipoRecebivelJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<TipoRecebivel> obtainByCodigo(String codigo) {
        return jpaRepository.findById(codigo).map(this::toDomain);
    }

    private TipoRecebivel toDomain(TipoRecebivelJpaEntity entity) {
        return new TipoRecebivel(entity.getCodigo(), entity.getNome(), entity.getSpread());
    }
}