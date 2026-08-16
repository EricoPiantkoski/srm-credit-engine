package com.srm.creditengine.cambio.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TaxaCambioTest {

    private static final ParMoedas PAR = new ParMoedas(new CodigoMoeda("USD"), new CodigoMoeda("BRL"));

    @Test
    void rejectsNullPar() {
        assertThatThrownBy(() -> new TaxaCambio(null, new BigDecimal("5.25"), Instant.parse("2026-08-14T16:00:00Z")))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("par must not be null");
    }

    @Test
    void rejectsNullTaxa() {
        assertThatThrownBy(() -> new TaxaCambio(PAR, null, Instant.parse("2026-08-14T16:00:00Z")))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("taxa must not be null");
    }

    @Test
    void rejectsNullVigencia() {
        assertThatThrownBy(() -> new TaxaCambio(PAR, new BigDecimal("5.25"), null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("vigencia must not be null");
    }

    @Test
    void rejectsZeroTaxa() {
        assertThatThrownBy(() -> new TaxaCambio(PAR, BigDecimal.ZERO, Instant.parse("2026-08-14T16:00:00Z")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("taxa must be positive");
    }

    @Test
    void rejectsNegativeTaxa() {
        assertThatThrownBy(() -> new TaxaCambio(PAR, new BigDecimal("-1.5"), Instant.parse("2026-08-14T16:00:00Z")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("taxa must be positive");
    }

    @Test
    void appliesScaleEight() {
        Instant vigencia = Instant.parse("2026-08-14T16:00:00Z");
        TaxaCambio taxa = new TaxaCambio(PAR, new BigDecimal("5.250000005"), vigencia);
        assertThat(taxa.par()).isEqualTo(PAR);
        assertThat(taxa.taxa()).isEqualByComparingTo("5.25000000");
        assertThat(taxa.vigencia()).isEqualTo(vigencia);
    }
}