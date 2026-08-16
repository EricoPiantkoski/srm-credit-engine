package com.srm.creditengine.cambio.domain;

import java.util.Optional;

public interface TaxaCambioProvider {
    Optional<TaxaCambio> obtain(ParMoedas par);
}