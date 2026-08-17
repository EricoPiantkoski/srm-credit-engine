package com.srm.creditengine.precificacao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TipoRecebivelTest {

    @Test
    void validatesFields() {
        TipoRecebivel tipo = new TipoRecebivel("DUPLICATA_MERCANTIL", "Duplicata Mercantil", new BigDecimal("0.015"));
        assertThat(tipo.codigo()).isEqualTo("DUPLICATA_MERCANTIL");
        assertThat(tipo.nome()).isEqualTo("Duplicata Mercantil");
        assertThat(tipo.spread()).isEqualByComparingTo("0.015000");
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new TipoRecebivel(null, "nome", new BigDecimal("0.01")))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TipoRecebivel("X", null, new BigDecimal("0.01")))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TipoRecebivel("X", "nome", null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNegativeSpread() {
        assertThatThrownBy(() -> new TipoRecebivel("X", "nome", new BigDecimal("-0.01")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be non-negative");
    }
}