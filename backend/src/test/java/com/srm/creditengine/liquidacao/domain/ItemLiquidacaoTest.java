package com.srm.creditengine.liquidacao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ItemLiquidacaoTest {

    @Test
    void createsValidItem() {
        ItemLiquidacao item = new ItemLiquidacao(
            1L, new BigDecimal("985.2200"), new BigDecimal("0.015000"), new BigDecimal("1.000000"),
            new BigDecimal("985.22"), "BRL", null);

        assertThat(item.recebivelId()).isEqualTo(1L);
        assertThat(item.valorPresente()).isEqualByComparingTo("985.2200");
        assertThat(item.spreadAplicado()).isEqualByComparingTo("0.015000");
        assertThat(item.prazoMeses()).isEqualByComparingTo("1.000000");
        assertThat(item.valorPagamento()).isEqualByComparingTo("985.22");
        assertThat(item.codigoMoedaPagamento()).isEqualTo("BRL");
        assertThat(item.taxaAplicada()).isNull();
    }

    @Test
    void rejectsNullRecebivelId() {
        assertThatThrownBy(() -> new ItemLiquidacao(
            null, new BigDecimal("10"), new BigDecimal("0.01"), new BigDecimal("1"),
            new BigDecimal("10"), "BRL", null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNegativeValorPresente() {
        assertThatThrownBy(() -> new ItemLiquidacao(
            1L, new BigDecimal("-1"), new BigDecimal("0.01"), new BigDecimal("1"),
            new BigDecimal("10"), "BRL", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("valorPresente");
    }

    @Test
    void rejectsNonPositivePrazo() {
        assertThatThrownBy(() -> new ItemLiquidacao(
            1L, new BigDecimal("10"), new BigDecimal("0.01"), BigDecimal.ZERO,
            new BigDecimal("10"), "BRL", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("prazoMeses");
    }

    @Test
    void rejectsNullCodigoMoedaPagamento() {
        assertThatThrownBy(() -> new ItemLiquidacao(
            1L, new BigDecimal("10"), new BigDecimal("0.01"), new BigDecimal("1"),
            new BigDecimal("10"), null, null))
            .isInstanceOf(NullPointerException.class);
    }
}