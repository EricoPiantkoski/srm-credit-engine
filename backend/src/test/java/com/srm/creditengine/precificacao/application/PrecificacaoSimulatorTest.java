package com.srm.creditengine.precificacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.srm.creditengine.precificacao.application.PrecificacaoSimulator.SimulatePrecificacaoInput;
import com.srm.creditengine.precificacao.domain.CambioGateway;
import com.srm.creditengine.precificacao.domain.MoedaCatalog;
import com.srm.creditengine.precificacao.domain.PrecificacaoStrategy;
import com.srm.creditengine.precificacao.domain.PrecificacaoStrategyResolver;
import com.srm.creditengine.precificacao.domain.Recebivel;
import com.srm.creditengine.precificacao.domain.ResultadoPrecificacao;
import com.srm.creditengine.precificacao.domain.Spread;
import com.srm.creditengine.precificacao.domain.TaxaCambioAplicada;
import com.srm.creditengine.precificacao.domain.exception.ExchangeRateUnavailableException;
import com.srm.creditengine.precificacao.domain.exception.UnknownCurrencyException;
import com.srm.creditengine.precificacao.domain.exception.UnknownReceivableTypeException;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import com.srm.creditengine.shared.domain.model.Dinheiro;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrecificacaoSimulatorTest {

    private static final Instant REFERENCE = Instant.parse("2026-08-16T12:00:00Z");
    private static final SimulatePrecificacaoInput INPUT = new SimulatePrecificacaoInput(
        "DUPLICATA_MERCANTIL", new BigDecimal("1000.00"), "BRL",
        LocalDate.of(2026, 9, 15), "BRL");

    @Mock
    private MoedaCatalog moedaCatalog;

    @Mock
    private PrecificacaoStrategyResolver resolver;

    private PrecificacaoSimulator simulator(PrecificacaoEngine engine) {
        return new PrecificacaoSimulator(resolver, engine, moedaCatalog);
    }

    @Test
    void simulatesWithoutPersisting() {
        when(moedaCatalog.scaleOf(new CodigoMoeda("BRL"))).thenReturn(2);
        PrecificacaoStrategy strategy = new PrecificacaoStrategy() {
            @Override
            public Spread spreadFor(Recebivel recebivel) {
                return new Spread(new BigDecimal("0.015"));
            }
        };
        when(resolver.resolveFor("DUPLICATA_MERCANTIL")).thenReturn(strategy);
        PrecificacaoEngine engine = new PrecificacaoEngine(new CambioGateway() {
            @Override
            public TaxaCambioAplicada convert(Dinheiro valor, CodigoMoeda moedaPagamento, Instant precificacaoReference) {
                throw new AssertionError("same-currency simulation must not convert");
            }
        }, BigDecimal.ZERO);

        ResultadoPrecificacao result = simulator(engine).simulate(INPUT, REFERENCE);

        assertThat(result.valorPresente().valor())
            .isCloseTo(new BigDecimal("985.22"), org.assertj.core.data.Offset.offset(new BigDecimal("0.01")));
        assertThat(result.valorLiquido()).isEqualTo(result.valorPresente());
    }

    @Test
    void throwsUnknownTipo() {
        when(moedaCatalog.scaleOf(new CodigoMoeda("BRL"))).thenReturn(2);
        when(resolver.resolveFor("DUPLICATA_MERCANTIL")).thenThrow(new UnknownReceivableTypeException("DUPLICATA_MERCANTIL"));

        assertThatThrownBy(() -> simulator(new PrecificacaoEngine(anyGateway(), BigDecimal.ZERO)).simulate(INPUT, REFERENCE))
            .isInstanceOf(UnknownReceivableTypeException.class);
    }

    @Test
    void throwsUnknownCurrency() {
        when(moedaCatalog.scaleOf(new CodigoMoeda("BRL"))).thenThrow(new UnknownCurrencyException("BRL"));

        assertThatThrownBy(() -> simulator(new PrecificacaoEngine(anyGateway(), BigDecimal.ZERO)).simulate(INPUT, REFERENCE))
            .isInstanceOf(UnknownCurrencyException.class);
        verify(resolver, never()).resolveFor(any());
    }

    @Test
    void crossCurrencyWithoutTaxaThrowsUnavailable() {
        PrecificacaoStrategy strategy = new PrecificacaoStrategy() {
            @Override
            public Spread spreadFor(Recebivel recebivel) {
                return new Spread(new BigDecimal("0.015"));
            }
        };
        when(resolver.resolveFor("DUPLICATA_MERCANTIL")).thenReturn(strategy);
        CambioGateway gateway = new CambioGateway() {
            @Override
            public TaxaCambioAplicada convert(Dinheiro valor, CodigoMoeda moedaPagamento, Instant precificacaoReference) {
                throw new ExchangeRateUnavailableException(valor.moeda(), moedaPagamento);
            }
        };
        SimulatePrecificacaoInput crossInput = new SimulatePrecificacaoInput(
            "DUPLICATA_MERCANTIL", new BigDecimal("1000.00"), "USD",
            LocalDate.of(2026, 9, 15), "BRL");
        when(moedaCatalog.scaleOf(new CodigoMoeda("USD"))).thenReturn(2);

        assertThatThrownBy(() -> simulator(new PrecificacaoEngine(gateway, BigDecimal.ZERO)).simulate(crossInput, REFERENCE))
            .isInstanceOf(ExchangeRateUnavailableException.class);
    }

    private static CambioGateway anyGateway() {
        return (valor, moedaPagamento, precificacaoReference) -> null;
    }
}