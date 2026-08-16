package com.srm.creditengine.cambio.infrastructure.adapter.out.persistence;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.cambio.domain.TaxaCambioRepository;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class TaxaCambioRepositoryAdapter implements TaxaCambioRepository {

    private final TaxaCambioJpaRepository jpaRepository;

    public TaxaCambioRepositoryAdapter(TaxaCambioJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<TaxaCambio> obtainVigente(ParMoedas par, Instant reference) {
        return jpaRepository
            .findFirstByCodigoBaseAndCodigoCotacaoAndVigenciaLessThanEqualOrderByVigenciaDesc(
                par.base().codigo(), par.cotacao().codigo(), reference)
            .map(this::toDomain);
    }

    @Override
    public boolean existsVigencia(ParMoedas par, Instant vigencia) {
        return jpaRepository.existsByCodigoBaseAndCodigoCotacaoAndVigencia(
            par.base().codigo(), par.cotacao().codigo(), vigencia);
    }

    @Override
    public void save(TaxaCambio taxa) {
        jpaRepository.save(new TaxaCambioJpaEntity(
            taxa.par().base().codigo(), taxa.par().cotacao().codigo(), taxa.taxa(), taxa.vigencia()));
    }

    private TaxaCambio toDomain(TaxaCambioJpaEntity entity) {
        return new TaxaCambio(
            new ParMoedas(new CodigoMoeda(entity.getCodigoBase()), new CodigoMoeda(entity.getCodigoCotacao())),
            entity.getTaxa(), entity.getVigencia());
    }
}