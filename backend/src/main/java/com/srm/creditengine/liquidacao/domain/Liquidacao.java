package com.srm.creditengine.liquidacao.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class Liquidacao {

    private final Long id;
    private final String chaveIdempotencia;
    private final StatusLiquidacao status;
    private final List<ItemLiquidacao> itens;
    private final Instant createdAt;

    public Liquidacao(Long id, String chaveIdempotencia, StatusLiquidacao status,
                      List<ItemLiquidacao> itens, Instant createdAt) {
        validate(chaveIdempotencia, status, itens, createdAt);
        this.id = id;
        this.chaveIdempotencia = chaveIdempotencia;
        this.status = status;
        this.itens = List.copyOf(itens);
        this.createdAt = createdAt;
    }

    private void validate(String chaveIdempotencia, StatusLiquidacao status,
                          List<ItemLiquidacao> itens, Instant createdAt) {
        Objects.requireNonNull(chaveIdempotencia, "chaveIdempotencia must not be null");
        if (chaveIdempotencia.isBlank()) {
            throw new IllegalArgumentException("chaveIdempotencia must not be blank");
        }
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(itens, "itens must not be null");
        if (itens.isEmpty()) {
            throw new IllegalArgumentException("itens must not be empty");
        }
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public Long id() { return id; }
    public String chaveIdempotencia() { return chaveIdempotencia; }
    public StatusLiquidacao status() { return status; }
    public List<ItemLiquidacao> itens() { return itens; }
    public Instant createdAt() { return createdAt; }
}