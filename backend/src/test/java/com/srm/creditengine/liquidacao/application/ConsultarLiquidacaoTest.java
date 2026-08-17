package com.srm.creditengine.liquidacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.srm.creditengine.liquidacao.domain.Liquidacao;
import com.srm.creditengine.liquidacao.domain.RepositorioLiquidacao;
import com.srm.creditengine.liquidacao.domain.exception.LiquidacaoNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultarLiquidacaoTest {

    @Mock
    private RepositorioLiquidacao repositorioLiquidacao;

    @Test
    void obtainByIdReturnsLiquidacao() {
        Liquidacao liquidacao = new Liquidacao(
            1L, "CHAVE-001", com.srm.creditengine.liquidacao.domain.StatusLiquidacao.LIQUIDADA,
            java.util.List.of(new com.srm.creditengine.liquidacao.domain.ItemLiquidacao(
                10L, new java.math.BigDecimal("985.22"), new java.math.BigDecimal("0.015000"),
                new java.math.BigDecimal("1.000000"), new java.math.BigDecimal("985.22"), "BRL", null)),
            java.time.Instant.parse("2026-08-16T12:00:00Z"));
        when(repositorioLiquidacao.obtainById(1L)).thenReturn(Optional.of(liquidacao));

        ConsultarLiquidacao service = new ConsultarLiquidacao(repositorioLiquidacao);

        assertThat(service.obtainById(1L)).isSameAs(liquidacao);
    }

    @Test
    void obtainByIdThrowsWhenMissing() {
        when(repositorioLiquidacao.obtainById(99L)).thenReturn(Optional.empty());

        ConsultarLiquidacao service = new ConsultarLiquidacao(repositorioLiquidacao);

        assertThatThrownBy(() -> service.obtainById(99L))
            .isInstanceOf(LiquidacaoNotFoundException.class)
            .hasMessageContaining("99");
    }
}