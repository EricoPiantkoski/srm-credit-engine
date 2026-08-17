package com.srm.creditengine.precificacao.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record TipoRecebivel(String codigo, String nome, BigDecimal spread) {

    public TipoRecebivel {
        Objects.requireNonNull(codigo, "codigo must not be null");
        Objects.requireNonNull(nome, "nome must not be null");
        Objects.requireNonNull(spread, "spread must not be null");
        if (spread.signum() < 0) {
            throw new IllegalArgumentException("spread must be non-negative, but was: " + spread);
        }
        spread = spread.setScale(6, RoundingMode.HALF_EVEN);
    }
}