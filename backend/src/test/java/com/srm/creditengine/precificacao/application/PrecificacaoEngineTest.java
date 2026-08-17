package com.srm.creditengine.precificacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.srm.creditengine.precificacao.domain.CambioGateway;
import com.srm.creditengine.precificacao.domain.PrecificacaoStrategy;
import com.srm.creditengine.precificacao.domain.Recebivel;
import com.srm.creditengine.precificacao.domain.ResultadoPrecificacao;
import com.srm.creditengine.precificacao.domain.Spread;
import com.srm.creditengine.precificacao.domain.TaxaCambioAplicada;
import com.srm.creditengine.precificacao.domain.exception.InvalidPricingException;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import com.srm.creditengine.shared.domain.model.Dinheiro;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrecificacaoEngineTest {

    private static final Instant REFERENCE = Instant.parse("2026-08-16T12:00:00Z");
    private static final CodigoMoeda BRL = new CodigoMoeda("BRL");
    private static final CodigoMoeda USD = new CodigoMoeda("USD");

    @Mock
    private CambioGateway cambioGateway;

    @Mock
    private PrecificacaoStrategy strategy;

    private PrecificacaoEngine engine(BigDecimal taxaBase) {
        return new PrecificacaoEngine(cambioGateway, taxaBase);
    }

    private Recebivel recebivel(String codigoMoeda, BigDecimal valorFace, LocalDate vencimento) {
        return new Recebivel(null, "REF", "DUPLICATA_MERCANTIL",
            new Dinheiro(valorFace, new CodigoMoeda(codigoMoeda), 2), vencimento, "Cedente", 0L);
    }

    private PrecificacaoStrategy strategy(BigDecimal spread) {
        return new PrecificacaoStrategy() {
            @Override
            public Spread spreadFor(Recebivel recebivel) {
                return new Spread(spread);
            }
        };
    }

    @Test
    void duplicataOneMonthFormula() {
        when(strategy.spreadFor(org.mockito.ArgumentMatchers.any()))
            .thenReturn(new Spread(new BigDecimal("0.015")));
        Recebivel recebivel = recebivel("BRL", new BigDecimal("1000.00"),
            LocalDate.of(2026, 9, 15));

        ResultadoPrecificacao result = engine(BigDecimal.ZERO)
            .price(recebivel, strategy, BRL, REFERENCE);

        assertThat(result.valorPresente().valor())
            .isCloseTo(new BigDecimal("985.22"), org.assertj.core.data.Offset.offset(new BigDecimal("0.01")));
        assertThat(result.spreadAplicado().valor()).isEqualByComparingTo("0.015000");
        assertThat(result.prazoMeses()).isEqualByComparingTo("1.000000");
        assertThat(result.valorLiquido()).isEqualTo(result.valorPresente());
        assertThat(result.taxaAplicada()).isNull();
        assertThat(result.vigenciaTaxa()).isNull();
        verifyNoInteractions(cambioGateway);
    }

    @Test
    void chequeOneMonthFormula() {
        when(strategy.spreadFor(org.mockito.ArgumentMatchers.any()))
            .thenReturn(new Spread(new BigDecimal("0.025")));
        Recebivel recebivel = recebivel("BRL", new BigDecimal("1000.00"),
            LocalDate.of(2026, 9, 15));

        ResultadoPrecificacao result = engine(BigDecimal.ZERO)
            .price(recebivel, strategy, BRL, REFERENCE);

        assertThat(result.valorPresente().valor())
            .isCloseTo(new BigDecimal("975.61"), org.assertj.core.data.Offset.offset(new BigDecimal("0.01")));
    }

    @Test
    void fractionalPrazoUsesMathPow() {
        when(strategy.spreadFor(org.mockito.ArgumentMatchers.any()))
            .thenReturn(new Spread(new BigDecimal("0.015")));
        Recebivel recebivel = recebivel("BRL", new BigDecimal("1000.00"),
            LocalDate.of(2026, 8, 31));

        ResultadoPrecificacao result = engine(BigDecimal.ZERO)
            .price(recebivel, strategy, BRL, REFERENCE);

        assertThat(result.prazoMeses()).isEqualByComparingTo("0.500000");
        assertThat(result.valorPresente().valor())
            .isCloseTo(new BigDecimal("992.59"), org.assertj.core.data.Offset.offset(new BigDecimal("0.01")));
    }

    @Test
    void crossCurrencyConvertsAtTheEnd() {
        when(strategy.spreadFor(org.mockito.ArgumentMatchers.any()))
            .thenReturn(new Spread(new BigDecimal("0.015")));
        Recebivel recebivel = recebivel("USD", new BigDecimal("1000.00"),
            LocalDate.of(2026, 9, 15));
        Dinheiro converted = new Dinheiro(new BigDecimal("4926.11"), BRL, 2);
        when(cambioGateway.convert(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(BRL),
                org.mockito.ArgumentMatchers.eq(REFERENCE)))
            .thenReturn(new TaxaCambioAplicada(converted, new BigDecimal("5.00"), REFERENCE));

        ResultadoPrecificacao result = engine(BigDecimal.ZERO)
            .price(recebivel, strategy, BRL, REFERENCE);

        assertThat(result.valorPresente().valor())
            .isCloseTo(new BigDecimal("985.22"), org.assertj.core.data.Offset.offset(new BigDecimal("0.01")));
        assertThat(result.valorLiquido().valor()).isEqualByComparingTo("4926.11");
        assertThat(result.valorLiquido().moeda()).isEqualTo(BRL);
        assertThat(result.taxaAplicada()).isEqualByComparingTo("5.00");
        assertThat(result.vigenciaTaxa()).isEqualTo(REFERENCE);
    }

    @Test
    void sameMoedaDoesNotCallCambioGateway() {
        when(strategy.spreadFor(org.mockito.ArgumentMatchers.any()))
            .thenReturn(new Spread(new BigDecimal("0.015")));
        Recebivel recebivel = recebivel("BRL", new BigDecimal("1000.00"),
            LocalDate.of(2026, 9, 15));

        engine(BigDecimal.ZERO).price(recebivel, strategy, BRL, REFERENCE);

        verifyNoInteractions(cambioGateway);
    }

    @Test
    void rejectsVencimentoInPast() {
        Recebivel recebivel = recebivel("BRL", new BigDecimal("1000.00"),
            LocalDate.of(2026, 8, 15));

        assertThatThrownBy(() -> engine(BigDecimal.ZERO)
            .price(recebivel, strategy, BRL, REFERENCE))
            .isInstanceOf(InvalidPricingException.class)
            .hasMessageContaining("vencimento must be in the future");
        verifyNoInteractions(cambioGateway);
    }

    @Test
    void rejectsVencimentoToday() {
        Recebivel recebivel = recebivel("BRL", new BigDecimal("1000.00"),
            LocalDate.of(2026, 8, 16));

        assertThatThrownBy(() -> engine(BigDecimal.ZERO)
            .price(recebivel, strategy, BRL, REFERENCE))
            .isInstanceOf(InvalidPricingException.class);
    }

    @Test
    void rejectsNullEngineDependencies() {
        assertThatThrownBy(() -> new PrecificacaoEngine(null, BigDecimal.ZERO))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PrecificacaoEngine(cambioGateway, null))
            .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PrecificacaoEngine(cambioGateway, new BigDecimal("-0.01")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("taxaBase must be non-negative");
    }

    @Test
    void appliesTaxaBase() {
        when(strategy.spreadFor(org.mockito.ArgumentMatchers.any()))
            .thenReturn(new Spread(new BigDecimal("0.015")));
        Recebivel recebivel = recebivel("BRL", new BigDecimal("1000.00"),
            LocalDate.of(2026, 9, 15));

        ResultadoPrecificacao result = engine(new BigDecimal("0.01"))
            .price(recebivel, strategy, BRL, REFERENCE);

        BigDecimal expected = new BigDecimal("1000.00").divide(
            BigDecimal.ONE.add(new BigDecimal("0.01")).add(new BigDecimal("0.015")).pow(1),
            2, RoundingMode.HALF_EVEN);
        assertThat(result.valorPresente().valor()).isEqualByComparingTo(expected);
    }
}