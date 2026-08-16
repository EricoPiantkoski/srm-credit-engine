package com.srm.creditengine.cambio.application;

import com.srm.creditengine.cambio.domain.MoedaRepository;
import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.cambio.domain.TaxaCambioRepository;
import com.srm.creditengine.cambio.domain.exception.ExchangeRateConflictException;
import com.srm.creditengine.cambio.domain.exception.UnknownCurrencyException;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import java.math.BigDecimal;
import java.time.Instant;

public class TaxaCambioUpdater {

    private final TaxaCambioRepository repository;
    private final MoedaRepository moedaRepository;

    public TaxaCambioUpdater(TaxaCambioRepository repository, MoedaRepository moedaRepository) {
        this.repository = repository;
        this.moedaRepository = moedaRepository;
    }

    public TaxaCambio update(ParMoedas par, BigDecimal taxa, Instant vigencia) {
        validateMoedas(par);
        TaxaCambio taxaCambio = new TaxaCambio(par, taxa, vigencia);
        validateVigenciaAvailable(par, taxaCambio.vigencia());
        repository.save(taxaCambio);
        return taxaCambio;
    }

    private void validateMoedas(ParMoedas par) {
        validateMoeda(par.base());
        validateMoeda(par.cotacao());
    }

    private void validateMoeda(CodigoMoeda moeda) {
        if (!moedaRepository.exists(moeda)) {
            throw new UnknownCurrencyException(moeda.codigo());
        }
    }

    private void validateVigenciaAvailable(ParMoedas par, Instant vigencia) {
        if (repository.existsVigencia(par, vigencia)) {
            throw new ExchangeRateConflictException(par, vigencia);
        }
    }
}