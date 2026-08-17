package com.srm.creditengine.liquidacao.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record ItemLiquidacao(
    Long recebivelId,
    BigDecimal valorPresente,
    BigDecimal spreadAplicado,
    BigDecimal prazoMeses,
    BigDecimal valorPagamento,
    String codigoMoedaPagamento,
    BigDecimal taxaAplicada) {

    public ItemLiquidacao {
        Objects.requireNonNull(recebivelId, "recebivelId must not be null");
        Objects.requireNonNull(valorPresente, "valorPresente must not be null");
        if (valorPresente.signum() < 0) {
            throw new IllegalArgumentException("valorPresente must be non-negative, but was: " + valorPresente);
        }
        Objects.requireNonNull(spreadAplicado, "spreadAplicado must not be null");
        if (spreadAplicado.signum() < 0) {
            throw new IllegalArgumentException("spreadAplicado must be non-negative, but was: " + spreadAplicado);
        }
        Objects.requireNonNull(prazoMeses, "prazoMeses must not be null");
        if (prazoMeses.signum() <= 0) {
            throw new IllegalArgumentException("prazoMeses must be positive, but was: " + prazoMeses);
        }
        Objects.requireNonNull(valorPagamento, "valorPagamento must not be null");
        Objects.requireNonNull(codigoMoedaPagamento, "codigoMoedaPagamento must not be null");
    }
}