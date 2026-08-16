package com.srm.creditengine.cambio.domain;

import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import java.util.Objects;

public record ParMoedas(CodigoMoeda base, CodigoMoeda cotacao) {

    public ParMoedas {
        Objects.requireNonNull(base, "base must not be null");
        Objects.requireNonNull(cotacao, "cotacao must not be null");
        if (base.equals(cotacao)) {
            throw new IllegalArgumentException("base and cotacao must differ, but both were: " + base);
        }
    }

    public boolean contem(CodigoMoeda moeda) {
        return base.equals(moeda) || cotacao.equals(moeda);
    }
}