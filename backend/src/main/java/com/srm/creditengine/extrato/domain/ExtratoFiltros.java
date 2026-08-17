package com.srm.creditengine.extrato.domain;

import java.time.LocalDate;
import java.util.Objects;

public record ExtratoFiltros(
    LocalDate dataInicial,
    LocalDate dataFinal,
    String status,
    String cedente,
    String codigoMoedaPagamento,
    Long lastId,
    int limit) {

    public ExtratoFiltros {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive, but was: " + limit);
        }
        if (lastId != null && lastId <= 0) {
            throw new IllegalArgumentException("lastId must be positive, but was: " + lastId);
        }
        Objects.requireNonNull(dataInicial, "dataInicial must not be null");
        Objects.requireNonNull(dataFinal, "dataFinal must not be null");
        if (dataFinal.isBefore(dataInicial)) {
            throw new IllegalArgumentException("dataFinal must not be before dataInicial");
        }
    }
}