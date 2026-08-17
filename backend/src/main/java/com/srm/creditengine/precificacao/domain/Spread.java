package com.srm.creditengine.precificacao.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Spread(BigDecimal valor) {

    public Spread {
        Objects.requireNonNull(valor, "valor must not be null");
        if (valor.signum() < 0) {
            throw new IllegalArgumentException("spread must be non-negative, but was: " + valor);
        }
        valor = valor.setScale(6, RoundingMode.HALF_EVEN);
    }
}