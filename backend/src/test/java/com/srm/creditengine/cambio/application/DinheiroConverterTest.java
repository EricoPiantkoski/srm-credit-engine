package com.srm.creditengine.cambio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.shared.domain.exception.IncompatibleCurrenciesException;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import com.srm.creditengine.shared.domain.model.Dinheiro;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DinheiroConverterTest {

    private static final ParMoedas USD_BRL = new ParMoedas(new CodigoMoeda("USD"), new CodigoMoeda("BRL"));
    private static final TaxaCambio TAXA = new TaxaCambio(USD_BRL, new BigDecimal("5.25"), Instant.parse("2026-08-14T16:00:00Z"));

    private final DinheiroConverter converter = new DinheiroConverter();

    @Test
    void convertsBaseToCotacao() {
        Dinheiro result = converter.convert(new Dinheiro(new BigDecimal("100.00"), new CodigoMoeda("USD"), 2), TAXA);

        assertThat(result.valor()).isEqualByComparingTo("525.00");
        assertThat(result.moeda()).isEqualTo(new CodigoMoeda("BRL"));
    }

    @Test
    void convertsCotacaoToBaseUsingInverse() {
        Dinheiro result = converter.convert(new Dinheiro(new BigDecimal("525.00"), new CodigoMoeda("BRL"), 2), TAXA);

        assertThat(result.valor()).isEqualByComparingTo("100.00");
        assertThat(result.moeda()).isEqualTo(new CodigoMoeda("USD"));
    }

    @Test
    void rejectsCurrencyNotInPair() {
        Dinheiro euros = new Dinheiro(new BigDecimal("100.00"), new CodigoMoeda("EUR"), 2);

        assertThatThrownBy(() -> converter.convert(euros, TAXA))
            .isInstanceOf(IncompatibleCurrenciesException.class);
    }
}