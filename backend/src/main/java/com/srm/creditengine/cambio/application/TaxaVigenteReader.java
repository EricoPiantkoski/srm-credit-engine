package com.srm.creditengine.cambio.application;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.cambio.domain.TaxaCambioProvider;
import com.srm.creditengine.cambio.domain.TaxaCambioRepository;
import java.time.Instant;
import java.util.Optional;

public class TaxaVigenteReader {

    private final TaxaCambioRepository repository;
    private final TaxaCambioProvider provider;

    public TaxaVigenteReader(TaxaCambioRepository repository, TaxaCambioProvider provider) {
        this.repository = repository;
        this.provider = provider;
    }

    public Optional<TaxaCambio> read(ParMoedas par, Instant reference) {
        return repository.obtainVigente(par, reference);
    }

    public Optional<TaxaCambio> readOrObtain(ParMoedas par, Instant reference) {
        return read(par, reference).or(() -> provider.obtain(par));
    }
}