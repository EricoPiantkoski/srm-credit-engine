package com.srm.creditengine.precificacao.infrastructure.adapter.out.cambio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.srm.creditengine.cambio.application.DinheiroConverter;
import com.srm.creditengine.cambio.application.TaxaVigenteReader;
import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.precificacao.domain.TaxaCambioAplicada;
import com.srm.creditengine.precificacao.domain.exception.ExchangeRateUnavailableException;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import com.srm.creditengine.shared.domain.model.Dinheiro;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CambioGatewayAdapterTest {

    private static final Instant REFERENCE = Instant.parse("2026-08-16T12:00:00Z");
    private static final CodigoMoeda BRL = new CodigoMoeda("BRL");
    private static final CodigoMoeda USD = new CodigoMoeda("USD");
    private static final TaxaCambio TAXA = new TaxaCambio(
        new ParMoedas(USD, BRL), new BigDecimal("5.00"), REFERENCE);

    @Mock
    private TaxaVigenteReader taxaVigenteReader;

    @Mock
    private DinheiroConverter dinheiroConverter;

    @Test
    void convertsViaReaderAndConverter() {
        Dinheiro valor = new Dinheiro(new BigDecimal("985.22"), USD, 2);
        Dinheiro converted = new Dinheiro(new BigDecimal("4926.10"), BRL, 2);
        when(taxaVigenteReader.readOrObtain(any(ParMoedas.class), eq(REFERENCE))).thenReturn(Optional.of(TAXA));
        when(dinheiroConverter.convert(valor, TAXA)).thenReturn(converted);

        TaxaCambioAplicada result = new CambioGatewayAdapter(taxaVigenteReader, dinheiroConverter)
            .convert(valor, BRL, REFERENCE);

        assertThat(result.valor()).isEqualTo(converted);
        assertThat(result.taxa()).isEqualByComparingTo("5.00");
        assertThat(result.vigencia()).isEqualTo(REFERENCE);
    }

    @Test
    void throwsWhenNoTaxaVigente() {
        Dinheiro valor = new Dinheiro(new BigDecimal("985.22"), USD, 2);
        when(taxaVigenteReader.readOrObtain(any(ParMoedas.class), eq(REFERENCE))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new CambioGatewayAdapter(taxaVigenteReader, dinheiroConverter)
            .convert(valor, BRL, REFERENCE))
            .isInstanceOf(ExchangeRateUnavailableException.class);
    }
}