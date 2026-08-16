package com.srm.creditengine.shared.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CodigoMoedaTest {

    @Test
    void acceptsValidIsoAlpha3Code() {
        assertThat(new CodigoMoeda("BRL").codigo()).isEqualTo("BRL");
        assertThat(new CodigoMoeda("USD").codigo()).isEqualTo("USD");
    }

    @Test
    void rejectsNullCode() {
        assertThatThrownBy(() -> new CodigoMoeda(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("codigo must not be null");
    }

    @Test
    void rejectsLowercaseCode() {
        assertThatThrownBy(() -> new CodigoMoeda("brl"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("currency code must be 3 uppercase letters, but was: brl");
    }

    @Test
    void rejectsCodeWithWrongLength() {
        assertThatThrownBy(() -> new CodigoMoeda("BR"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("currency code must be 3 uppercase letters, but was: BR");
    }

    @Test
    void rejectsCodeWithDigits() {
        assertThatThrownBy(() -> new CodigoMoeda("BR1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("currency code must be 3 uppercase letters, but was: BR1");
    }
}