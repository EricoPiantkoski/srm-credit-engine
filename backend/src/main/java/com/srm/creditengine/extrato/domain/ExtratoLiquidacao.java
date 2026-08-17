package com.srm.creditengine.extrato.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record ExtratoLiquidacao(
    Long itemId,
    Long liquidacaoId,
    String chaveIdempotencia,
    String status,
    Instant createdAt,
    Long recebivelId,
    String cedente,
    BigDecimal valorPresente,
    BigDecimal spreadAplicado,
    BigDecimal prazoMeses,
    BigDecimal valorPagamento,
    String codigoMoedaPagamento,
    BigDecimal taxaAplicada) {}