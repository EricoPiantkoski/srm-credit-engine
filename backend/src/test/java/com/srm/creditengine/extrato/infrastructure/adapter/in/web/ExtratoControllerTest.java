package com.srm.creditengine.extrato.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.srm.creditengine.extrato.application.ExtratoLiquidacoes;
import com.srm.creditengine.extrato.domain.ExtratoFiltros;
import com.srm.creditengine.extrato.domain.ExtratoLiquidacao;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WithMockUser(roles = "ADMIN")
@WebMvcTest(ExtratoController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExtratoControllerTest {

    private static final ExtratoLiquidacao EXTRATO = new ExtratoLiquidacao(
        1L, 1L, "CHAVE-001", "LIQUIDADA", Instant.parse("2026-08-16T12:00:00Z"),
        10L, "Cedente A", new BigDecimal("985.2200"), new BigDecimal("0.015000"),
        new BigDecimal("1.000000"), new BigDecimal("985.22"), "BRL", null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExtratoLiquidacoes extratoLiquidacoes;

    @Test
    void extratoReturns200() throws Exception {
        when(extratoLiquidacoes.consultar(any())).thenReturn(List.of(EXTRATO));

        mockMvc.perform(get("/api/liquidacoes/extrato")
                .param("dataInicial", "2026-08-01")
                .param("dataFinal", "2026-08-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].itemId").value(1))
            .andExpect(jsonPath("$[0].liquidacaoId").value(1))
            .andExpect(jsonPath("$[0].chaveIdempotencia").value("CHAVE-001"))
            .andExpect(jsonPath("$[0].status").value("LIQUIDADA"))
            .andExpect(jsonPath("$[0].cedente").value("Cedente A"))
            .andExpect(jsonPath("$[0].valorPagamento").value(985.22))
            .andExpect(jsonPath("$[0].codigoMoedaPagamento").value("BRL"));
    }

    @Test
    void extratoWithFiltersReturns200() throws Exception {
        when(extratoLiquidacoes.consultar(any())).thenReturn(List.of(EXTRATO));

        mockMvc.perform(get("/api/liquidacoes/extrato")
                .param("status", "LIQUIDADA")
                .param("cedente", "Cedente A")
                .param("codigoMoedaPagamento", "BRL")
                .param("lastId", "5")
                .param("limit", "10"))
            .andExpect(status().isOk());
    }

    @Test
    void extratoWithoutDatesDefaultsToFullRange() throws Exception {
        when(extratoLiquidacoes.consultar(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/liquidacoes/extrato"))
            .andExpect(status().isOk());
    }

    @Test
    void extratoRejectsLimitAboveMax() throws Exception {
        mockMvc.perform(get("/api/liquidacoes/extrato")
                .param("dataInicial", "2026-08-01")
                .param("dataFinal", "2026-08-31")
                .param("limit", String.valueOf(ExtratoFiltros.MAX_LIMIT + 1)))
            .andExpect(status().isBadRequest());
    }
}