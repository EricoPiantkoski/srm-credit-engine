package com.srm.creditengine.shared.domain.model;

import com.srm.creditengine.shared.domain.exception.IncompatibleCurrenciesException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Dinheiro(BigDecimal valor, CodigoMoeda moeda, int escala) {

    public Dinheiro {
        Objects.requireNonNull(valor, "valor must not be null");
        Objects.requireNonNull(moeda, "moeda must not be null");
        if (escala < 0) {
            throw new IllegalArgumentException(
                "escala must be non-negative, but was: " + escala);
        }
        valor = valor.setScale(escala, RoundingMode.HALF_EVEN);
    }

    public Dinheiro add(Dinheiro other) {
        validateSameMoeda(other);
        return new Dinheiro(valor.add(other.valor), moeda, escala);
    }

    public Dinheiro multiply(BigDecimal factor) {
        validateFactor(factor);
        return new Dinheiro(valor.multiply(factor), moeda, escala);
    }

    private void validateSameMoeda(Dinheiro other) {
        if (!moeda.equals(other.moeda)) {
            throw new IncompatibleCurrenciesException(moeda, other.moeda);
        }
    }

    private void validateFactor(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor must not be null");
    }
}