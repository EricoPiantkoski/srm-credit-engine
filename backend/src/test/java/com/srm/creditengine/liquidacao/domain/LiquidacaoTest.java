package com.srm.creditengine.liquidacao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class LiquidacaoTest {

    private static final Instant NOW = Instant.parse("2026-08-16T12:00:00Z");

    private static final ItemLiquidacao ITEM = new ItemLiquidacao(
        1L, new BigDecimal("985.2200"), new BigDecimal("0.015000"), new BigDecimal("1.000000"),
        new BigDecimal("985.22"), "BRL", null);

    @Test
    void createsLiquidacaoWithItens() {
        Liquidacao liquidacao = new Liquidacao(
            1L, "CHAVE-001", StatusLiquidacao.LIQUIDADA, List.of(ITEM), NOW);

        assertThat(liquidacao.id()).isEqualTo(1L);
        assertThat(liquidacao.chaveIdempotencia()).isEqualTo("CHAVE-001");
        assertThat(liquidacao.status()).isEqualTo(StatusLiquidacao.LIQUIDADA);
        assertThat(liquidacao.createdAt()).isEqualTo(NOW);
        assertThat(liquidacao.itens()).containsExactly(ITEM);
        assertThat(liquidacao.itens()).isNotSameAs(List.of(ITEM));
    }

    @Test
    void rejectsBlankChaveIdempotencia() {
        assertThatThrownBy(() -> new Liquidacao(
            null, " ", StatusLiquidacao.LIQUIDADA, List.of(ITEM), NOW))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("chaveIdempotencia");
    }

    @Test
    void rejectsNullStatus() {
        assertThatThrownBy(() -> new Liquidacao(
            null, "CHAVE-001", null, List.of(ITEM), NOW))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("status");
    }

    @Test
    void rejectsEmptyItens() {
        assertThatThrownBy(() -> new Liquidacao(
            null, "CHAVE-001", StatusLiquidacao.LIQUIDADA, List.of(), NOW))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("itens");
    }

    @Test
    void rejectsNullCreatedAt() {
        assertThatThrownBy(() -> new Liquidacao(
            null, "CHAVE-001", StatusLiquidacao.LIQUIDADA, List.of(ITEM), null))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("createdAt");
    }
}