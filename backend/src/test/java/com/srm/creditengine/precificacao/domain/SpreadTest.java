package com.srm.creditengine.precificacao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SpreadTest {

    @Test
    void roundsToScale6() {
        assertThat(new Spread(new BigDecimal("0.0150001")).valor()).isEqualByComparingTo("0.015000");
        assertThat(new Spread(new BigDecimal("0.015")).valor()).isEqualByComparingTo("0.015000");
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> new Spread(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNegative() {
        assertThatThrownBy(() -> new Spread(new BigDecimal("-0.01")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be non-negative");
    }
}