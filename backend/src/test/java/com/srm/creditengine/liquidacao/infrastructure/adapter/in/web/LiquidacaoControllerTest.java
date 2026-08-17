package com.srm.creditengine.liquidacao.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.srm.creditengine.liquidacao.application.ConsultarLiquidacao;
import com.srm.creditengine.liquidacao.application.LiquidarLote;
import com.srm.creditengine.liquidacao.domain.ItemLiquidacao;
import com.srm.creditengine.liquidacao.domain.Liquidacao;
import com.srm.creditengine.liquidacao.domain.StatusLiquidacao;
import com.srm.creditengine.liquidacao.domain.exception.LiquidacaoConflictException;
import com.srm.creditengine.liquidacao.domain.exception.LiquidacaoNotFoundException;
import com.srm.creditengine.liquidacao.domain.exception.RecebivelNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LiquidacaoController.class)
class LiquidacaoControllerTest {

    private static final Liquidacao LIQUIDACAO = new Liquidacao(
        1L, "9f8e7d6c-5b4a-4c3d-8e2f-1a2b3c4d5e6f", StatusLiquidacao.LIQUIDADA,
        List.of(new ItemLiquidacao(10L, new BigDecimal("985.2200"), new BigDecimal("0.015000"),
            new BigDecimal("1.000000"), new BigDecimal("985.22"), "BRL", null)),
        Instant.parse("2026-08-16T12:00:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LiquidarLote liquidarLote;

    @MockitoBean
    private ConsultarLiquidacao consultarLiquidacao;

    @Test
    void createReturns201() throws Exception {
        when(liquidarLote.liquidar(any())).thenReturn(LIQUIDACAO);

        mockMvc.perform(post("/api/liquidacoes")
                .contentType(APPLICATION_JSON)
                .content("{\"chaveIdempotencia\":\"9f8e7d6c-5b4a-4c3d-8e2f-1a2b3c4d5e6f\",\"codigoMoedaPagamento\":\"BRL\"," +
                    "\"recebiveisIds\":[10]}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.chaveIdempotencia").value("9f8e7d6c-5b4a-4c3d-8e2f-1a2b3c4d5e6f"))
            .andExpect(jsonPath("$.status").value("LIQUIDADA"))
            .andExpect(jsonPath("$.itens[0].recebivelId").value(10))
            .andExpect(jsonPath("$.itens[0].valorPresente").value(985.2200))
            .andExpect(jsonPath("$.itens[0].codigoMoedaPagamento").value("BRL"));
    }

    @Test
    void createReturns400OnInvalidBody() throws Exception {
        mockMvc.perform(post("/api/liquidacoes")
                .contentType(APPLICATION_JSON)
                .content("{\"chaveIdempotencia\":\"\",\"codigoMoedaPagamento\":\"brl\"," +
                    "\"recebiveisIds\":[]}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createReturns400OnNonUuidChave() throws Exception {
        mockMvc.perform(post("/api/liquidacoes")
                .contentType(APPLICATION_JSON)
                .content("{\"chaveIdempotencia\":\"CHAVE-001\",\"codigoMoedaPagamento\":\"BRL\"," +
                    "\"recebiveisIds\":[10]}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createReturns409OnConflict() throws Exception {
        when(liquidarLote.liquidar(any()))
            .thenThrow(new LiquidacaoConflictException("9f8e7d6c-5b4a-4c3d-8e2f-1a2b3c4d5e6f"));

        mockMvc.perform(post("/api/liquidacoes")
                .contentType(APPLICATION_JSON)
                .content("{\"chaveIdempotencia\":\"9f8e7d6c-5b4a-4c3d-8e2f-1a2b3c4d5e6f\",\"codigoMoedaPagamento\":\"BRL\"," +
                    "\"recebiveisIds\":[10]}"))
            .andExpect(status().isConflict());
    }

    @Test
    void createReturns422OnMissingRecebivel() throws Exception {
        when(liquidarLote.liquidar(any()))
            .thenThrow(new RecebivelNotFoundException(99L));

        mockMvc.perform(post("/api/liquidacoes")
                .contentType(APPLICATION_JSON)
                .content("{\"chaveIdempotencia\":\"9f8e7d6c-5b4a-4c3d-8e2f-1a2b3c4d5e6f\",\"codigoMoedaPagamento\":\"BRL\"," +
                    "\"recebiveisIds\":[99]}"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void obtainReturns200() throws Exception {
        when(consultarLiquidacao.obtainById(1L)).thenReturn(LIQUIDACAO);

        mockMvc.perform(get("/api/liquidacoes/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.chaveIdempotencia").value("9f8e7d6c-5b4a-4c3d-8e2f-1a2b3c4d5e6f"))
            .andExpect(jsonPath("$.itens[0].recebivelId").value(10));
    }

    @Test
    void obtainReturns404() throws Exception {
        when(consultarLiquidacao.obtainById(99L))
            .thenThrow(new LiquidacaoNotFoundException(99L));

        mockMvc.perform(get("/api/liquidacoes/99"))
            .andExpect(status().isNotFound());
    }
}