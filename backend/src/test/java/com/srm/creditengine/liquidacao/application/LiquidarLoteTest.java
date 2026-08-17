package com.srm.creditengine.liquidacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.srm.creditengine.liquidacao.domain.Liquidacao;
import com.srm.creditengine.liquidacao.domain.RepositorioLiquidacao;
import com.srm.creditengine.liquidacao.domain.exception.LiquidacaoConflictException;
import com.srm.creditengine.liquidacao.domain.exception.LiquidacaoVersionConflictException;
import com.srm.creditengine.liquidacao.domain.exception.RecebivelNotFoundException;
import com.srm.creditengine.precificacao.application.PrecificacaoEngine;
import com.srm.creditengine.precificacao.domain.PrecificacaoStrategy;
import com.srm.creditengine.precificacao.domain.PrecificacaoStrategyResolver;
import com.srm.creditengine.precificacao.domain.Recebivel;
import com.srm.creditengine.precificacao.domain.RecebivelRepository;
import com.srm.creditengine.precificacao.domain.ResultadoPrecificacao;
import com.srm.creditengine.precificacao.domain.Spread;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import com.srm.creditengine.shared.domain.model.Dinheiro;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LiquidarLoteTest {

    @Mock
    private RepositorioLiquidacao repositorioLiquidacao;

    @Mock
    private RecebivelRepository recebivelRepository;

    @Mock
    private PrecificacaoStrategyResolver strategyResolver;

    @Mock
    private PrecificacaoEngine engine;

    private LiquidarLote liquidarLote;

    @BeforeEach
    void setUp() {
        liquidarLote = new LiquidarLote(repositorioLiquidacao, recebivelRepository, strategyResolver, engine);
    }

    private Recebivel recebivel() {
        return new Recebivel(10L, "REF-010", "DUPLICATA_MERCANTIL",
            new Dinheiro(new BigDecimal("1000.00"), new CodigoMoeda("BRL"), 2),
            LocalDate.of(2026, 9, 15), "Cedente", 3L);
    }

    private ResultadoPrecificacao resultado() {
        Dinheiro valorPresente = new Dinheiro(new BigDecimal("985.22"), new CodigoMoeda("BRL"), 2);
        return new ResultadoPrecificacao(
            valorPresente, new Spread(new BigDecimal("0.015")), new BigDecimal("1.000000"),
            valorPresente, null, null);
    }

    @Test
    void liquidarPrecificaAndSaves() {
        Recebivel recebivel = recebivel();
        when(repositorioLiquidacao.existsChaveIdempotencia("9f8e7d6c-5b4a-4c3d-8e2f-1a2b3c4d5e6f")).thenReturn(false);
        when(recebivelRepository.obtainById(10L)).thenReturn(Optional.of(recebivel));
        when(recebivelRepository.marcarLiquidado(10L, 3L)).thenReturn(true);
        when(strategyResolver.resolveFor("DUPLICATA_MERCANTIL")).thenReturn(new PrecificacaoStrategy() {
            @Override
            public Spread spreadFor(Recebivel r) {
                return new Spread(new BigDecimal("0.015"));
            }
        });
        when(engine.price(eq(recebivel), any(), eq(new CodigoMoeda("BRL")), any()))
            .thenReturn(resultado());
        when(repositorioLiquidacao.save(any(Liquidacao.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        var liquidacao = liquidarLote.liquidar(
            new LiquidarLote.LiquidarLoteInput("9f8e7d6c-5b4a-4c3d-8e2f-1a2b3c4d5e6f", "BRL", List.of(10L)));

        assertThat(liquidacao.chaveIdempotencia()).isEqualTo("9f8e7d6c-5b4a-4c3d-8e2f-1a2b3c4d5e6f");
        assertThat(liquidacao.itens()).hasSize(1);
        assertThat(liquidacao.itens().get(0).recebivelId()).isEqualTo(10L);
        assertThat(liquidacao.itens().get(0).valorPresente()).isEqualByComparingTo("985.22");
        assertThat(liquidacao.itens().get(0).spreadAplicado()).isEqualByComparingTo("0.015000");
        assertThat(liquidacao.itens().get(0).prazoMeses()).isEqualByComparingTo("1.000000");
        assertThat(liquidacao.itens().get(0).valorPagamento()).isEqualByComparingTo("985.22");
        assertThat(liquidacao.itens().get(0).codigoMoedaPagamento()).isEqualTo("BRL");
        assertThat(liquidacao.itens().get(0).taxaAplicada()).isNull();
        verify(repositorioLiquidacao).save(any());
    }

    @Test
    void rejectsDuplicateChaveIdempotencia() {
        when(repositorioLiquidacao.existsChaveIdempotencia("9f8e7d6c-5b4a-4c3d-8e2f-1a2b3c4d5e6f")).thenReturn(true);

        assertThatThrownBy(() -> liquidarLote.liquidar(
            new LiquidarLote.LiquidarLoteInput("9f8e7d6c-5b4a-4c3d-8e2f-1a2b3c4d5e6f", "BRL", List.of(10L))))
            .isInstanceOf(LiquidacaoConflictException.class)
            .hasMessageContaining("9f8e7d6c-5b4a-4c3d-8e2f-1a2b3c4d5e6f");
        verify(repositorioLiquidacao, never()).save(any());
    }

    @Test
    void rejectsMissingRecebivel() {
        when(repositorioLiquidacao.existsChaveIdempotencia("9f8e7d6c-5b4a-4c3d-8e2f-1a2b3c4d5e6f")).thenReturn(false);
        when(recebivelRepository.obtainById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> liquidarLote.liquidar(
            new LiquidarLote.LiquidarLoteInput("9f8e7d6c-5b4a-4c3d-8e2f-1a2b3c4d5e6f", "BRL", List.of(10L))))
            .isInstanceOf(RecebivelNotFoundException.class)
            .hasMessageContaining("10");
    }

    @Test
    void rejectsVersionConflict() {
        when(repositorioLiquidacao.existsChaveIdempotencia("9f8e7d6c-5b4a-4c3d-8e2f-1a2b3c4d5e6f")).thenReturn(false);
        when(recebivelRepository.obtainById(10L)).thenReturn(Optional.of(recebivel()));
        when(recebivelRepository.marcarLiquidado(10L, 3L)).thenReturn(false);

        assertThatThrownBy(() -> liquidarLote.liquidar(
            new LiquidarLote.LiquidarLoteInput("9f8e7d6c-5b4a-4c3d-8e2f-1a2b3c4d5e6f", "BRL", List.of(10L))))
            .isInstanceOf(LiquidacaoVersionConflictException.class)
            .hasMessageContaining("Reprocess");
        verify(repositorioLiquidacao, never()).save(any());
    }

    @Test
    void rejectsEmptyRecebiveis() {
        assertThatThrownBy(() -> new LiquidarLote.LiquidarLoteInput("9f8e7d6c-5b4a-4c3d-8e2f-1a2b3c4d5e6f", "BRL", List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("recebiveisIds");
    }

    @Test
    void rejectsNonUuidChaveIdempotencia() {
        assertThatThrownBy(() -> new LiquidarLote.LiquidarLoteInput("CHAVE-001", "BRL", List.of(10L)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("UUID");
    }
}