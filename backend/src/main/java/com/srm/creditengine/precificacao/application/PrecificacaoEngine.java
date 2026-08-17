package com.srm.creditengine.precificacao.application;

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
import java.time.ZoneId;
import java.util.Objects;

public class PrecificacaoEngine {

    private static final BigDecimal DAYS_PER_MONTH = new BigDecimal("30");
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    private final CambioGateway cambioGateway;
    private final BigDecimal taxaBase;

    public PrecificacaoEngine(CambioGateway cambioGateway, BigDecimal taxaBase) {
        Objects.requireNonNull(cambioGateway, "cambioGateway must not be null");
        Objects.requireNonNull(taxaBase, "taxaBase must not be null");
        if (taxaBase.signum() < 0) {
            throw new IllegalArgumentException("taxaBase must be non-negative, but was: " + taxaBase);
        }
        this.cambioGateway = cambioGateway;
        this.taxaBase = taxaBase;
    }

    public ResultadoPrecificacao price(Recebivel recebivel, PrecificacaoStrategy strategy,
                                       CodigoMoeda moedaPagamento, Instant precificacaoReference) {
        validateDataVencimentoInFuture(recebivel, precificacaoReference);
        Spread spread = strategy.spreadFor(recebivel);
        BigDecimal prazoMeses = prazoMeses(recebivel, precificacaoReference);
        validatePrazo(prazoMeses);
        BigDecimal factor = BigDecimal.ONE.add(taxaBase).add(spread.valor());
        BigDecimal discount = pow(factor, prazoMeses);
        Dinheiro valorPresente = new Dinheiro(
            recebivel.valorFace().valor().divide(discount, recebivel.valorFace().escala(), RoundingMode.HALF_EVEN),
            recebivel.valorFace().moeda(), recebivel.valorFace().escala());

        if (moedaPagamento.equals(recebivel.valorFace().moeda())) {
            return new ResultadoPrecificacao(valorPresente, spread, prazoMeses, valorPresente, null, null);
        }
        TaxaCambioAplicada conversion = cambioGateway.convert(valorPresente, moedaPagamento, precificacaoReference);
        return new ResultadoPrecificacao(valorPresente, spread, prazoMeses,
            conversion.valor(), conversion.taxa(), conversion.vigencia());
    }

    private void validateDataVencimentoInFuture(Recebivel recebivel, Instant precificacaoReference) {
        LocalDate today = precificacaoReference.atZone(BUSINESS_ZONE).toLocalDate();
        if (!recebivel.dataVencimento().isAfter(today)) {
            throw new InvalidPricingException(
                "vencimento must be in the future, but was: " + recebivel.dataVencimento());
        }
    }

    private BigDecimal prazoMeses(Recebivel recebivel, Instant precificacaoReference) {
        LocalDate today = precificacaoReference.atZone(BUSINESS_ZONE).toLocalDate();
        long days = recebivel.prazoInDays(today);
        return BigDecimal.valueOf(days).divide(DAYS_PER_MONTH, 6, RoundingMode.HALF_EVEN);
    }

    private void validatePrazo(BigDecimal prazoMeses) {
        if (prazoMeses.signum() <= 0) {
            throw new InvalidPricingException("prazo must be positive, but was: " + prazoMeses);
        }
    }

    private BigDecimal pow(BigDecimal base, BigDecimal exponent) {
        if (exponent.stripTrailingZeros().scale() <= 0) {
            return base.pow(exponent.intValueExact());
        }
        return BigDecimal.valueOf(Math.pow(base.doubleValue(), exponent.doubleValue()));
    }
}