package com.srm.creditengine.shared.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.srm.creditengine.shared.domain.exception.IncompatibleCurrenciesException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DinheiroTest {

    private static final CodigoMoeda BRL = new CodigoMoeda("BRL");
    private static final CodigoMoeda USD = new CodigoMoeda("USD");

    @Test
    void rejectsNullValor() {
        assertThatThrownBy(() -> new Dinheiro(null, BRL, 2))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("valor must not be null");
    }

    @Test
    void rejectsNullMoeda() {
        assertThatThrownBy(() -> new Dinheiro(new BigDecimal("10.00"), null, 2))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("moeda must not be null");
    }

    @Test
    void rejectsNegativeEscala() {
        assertThatThrownBy(() -> new Dinheiro(new BigDecimal("10.00"), BRL, -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("escala must be non-negative, but was: -1");
    }

    @Test
    void appliesEscalaWithHalfEven() {
        Dinheiro dinheiro = new Dinheiro(new BigDecimal("2.555"), BRL, 2);
        assertThat(dinheiro.valor()).isEqualByComparingTo("2.56");
    }

    @Test
    void sumsAmountsOfSameCurrency() {
        Dinheiro a = new Dinheiro(new BigDecimal("10.00"), BRL, 2);
        Dinheiro b = new Dinheiro(new BigDecimal("5.00"), BRL, 2);
        Dinheiro result = a.add(b);
        assertThat(result.valor()).isEqualByComparingTo("15.00");
        assertThat(result.moeda()).isEqualTo(BRL);
    }

    @Test
    void rejectsSumOfDifferentCurrencies() {
        Dinheiro brl = new Dinheiro(new BigDecimal("10.00"), BRL, 2);
        Dinheiro usd = new Dinheiro(new BigDecimal("5.00"), USD, 2);
        assertThatThrownBy(() -> brl.add(usd))
            .isInstanceOf(IncompatibleCurrenciesException.class)
            .hasMessageContaining("Cannot operate on amounts of different currencies");
    }

    @Test
    void multipliesPreservingCurrencyAndEscala() {
        Dinheiro dinheiro = new Dinheiro(new BigDecimal("10.00"), BRL, 2);
        Dinheiro result = dinheiro.multiply(new BigDecimal("1.5"));
        assertThat(result.valor()).isEqualByComparingTo("15.00");
        assertThat(result.moeda()).isEqualTo(BRL);
    }

    @Test
    void rejectsNullFactor() {
        Dinheiro dinheiro = new Dinheiro(new BigDecimal("10.00"), BRL, 2);
        assertThatThrownBy(() -> dinheiro.multiply(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("factor must not be null");
    }
}