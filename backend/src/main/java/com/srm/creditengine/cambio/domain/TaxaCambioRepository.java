package com.srm.creditengine.cambio.domain;

import java.time.Instant;
import java.util.Optional;

public interface TaxaCambioRepository {
    Optional<TaxaCambio> obtainVigente(ParMoedas par, Instant reference);
    boolean existsVigencia(ParMoedas par, Instant vigencia);
    void save(TaxaCambio taxa);
}