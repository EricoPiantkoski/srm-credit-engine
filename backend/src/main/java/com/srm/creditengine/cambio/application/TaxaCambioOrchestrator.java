package com.srm.creditengine.cambio.application;

import com.srm.creditengine.cambio.domain.MoedaRepository;
import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.cambio.domain.TaxaCambioProvider;
import com.srm.creditengine.cambio.domain.TaxaCambioRepository;
import com.srm.creditengine.cambio.domain.exception.ExchangeRateProviderUnavailableException;
import com.srm.creditengine.cambio.domain.exception.UnknownCurrencyException;
import java.util.Optional;

public class TaxaCambioOrchestrator {

    private final TaxaCambioProvider provider;
    private final TaxaCambioRepository repository;
    private final MoedaRepository moedaRepository;

    public TaxaCambioOrchestrator(TaxaCambioProvider provider, TaxaCambioRepository repository,
                                  MoedaRepository moedaRepository) {
        this.provider = provider;
        this.repository = repository;
        this.moedaRepository = moedaRepository;
    }

    public TaxaCambio orchestrate(ParMoedas par) {
        validateMoedas(par);
        Optional<TaxaCambio> taxa = provider.obtain(par);
        if (taxa.isEmpty()) {
            throw new ExchangeRateProviderUnavailableException(par);
        }
        TaxaCambio obtained = taxa.get();
        if (!repository.existsVigencia(par, obtained.vigencia())) {
            repository.save(obtained);
        }
        return obtained;
    }

    private void validateMoedas(ParMoedas par) {
        if (!moedaRepository.exists(par.base())) {
            throw new UnknownCurrencyException(par.base().codigo());
        }
        if (!moedaRepository.exists(par.cotacao())) {
            throw new UnknownCurrencyException(par.cotacao().codigo());
        }
    }
}