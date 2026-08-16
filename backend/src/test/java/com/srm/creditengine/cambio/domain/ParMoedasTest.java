package com.srm.creditengine.cambio.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import org.junit.jupiter.api.Test;

class ParMoedasTest {

    private static final CodigoMoeda BRL = new CodigoMoeda("BRL");
    private static final CodigoMoeda USD = new CodigoMoeda("USD");

    @Test
    void acceptsValidPair() {
        ParMoedas par = new ParMoedas(USD, BRL);
        assertThat(par.base()).isEqualTo(USD);
        assertThat(par.cotacao()).isEqualTo(BRL);
    }

    @Test
    void rejectsNullBase() {
        assertThatThrownBy(() -> new ParMoedas(null, BRL))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("base must not be null");
    }

    @Test
    void rejectsNullCotacao() {
        assertThatThrownBy(() -> new ParMoedas(USD, null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("cotacao must not be null");
    }

    @Test
    void rejectsSameCurrency() {
        assertThatThrownBy(() -> new ParMoedas(USD, USD))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("base and cotacao must differ");
    }

    @Test
    void contemReturnsTrueForBothCurrencies() {
        ParMoedas par = new ParMoedas(USD, BRL);
        assertThat(par.contem(USD)).isTrue();
        assertThat(par.contem(BRL)).isTrue();
    }

    @Test
    void contemReturnsFalseForOtherCurrency() {
        ParMoedas par = new ParMoedas(USD, BRL);
        assertThat(par.contem(new CodigoMoeda("EUR"))).isFalse();
    }
}