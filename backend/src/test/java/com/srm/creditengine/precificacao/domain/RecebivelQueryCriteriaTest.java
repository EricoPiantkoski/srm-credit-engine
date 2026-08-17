package com.srm.creditengine.precificacao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RecebivelQueryCriteriaTest {

    @Test
    void allowsNullFilters() {
        RecebivelQueryCriteria criteria = new RecebivelQueryCriteria(null, null, null, 0, 20);
        assertThat(criteria.cedente()).isNull();
        assertThat(criteria.codigoMoeda()).isNull();
        assertThat(criteria.codigoTipo()).isNull();
        assertThat(criteria.page()).isZero();
        assertThat(criteria.size()).isEqualTo(20);
    }

    @Test
    void rejectsNegativePage() {
        assertThatThrownBy(() -> new RecebivelQueryCriteria(null, null, null, -1, 20))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("page must be non-negative");
    }

    @Test
    void rejectsNonPositiveSize() {
        assertThatThrownBy(() -> new RecebivelQueryCriteria(null, null, null, 0, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("size must be positive");
    }
}