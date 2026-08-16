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

    public Dinheiro somar(Dinheiro outro) {
        validarMesmaMoeda(outro);
        return new Dinheiro(valor.add(outro.valor), moeda, escala);
    }

    public Dinheiro multiplicar(BigDecimal fator) {
        validarFator(fator);
        return new Dinheiro(valor.multiply(fator), moeda, escala);
    }

    private void validarMesmaMoeda(Dinheiro outro) {
        if (!moeda.equals(outro.moeda)) {
            throw new IncompatibleCurrenciesException(moeda, outro.moeda);
        }
    }

    private void validarFator(BigDecimal fator) {
        Objects.requireNonNull(fator, "fator must not be null");
    }
}