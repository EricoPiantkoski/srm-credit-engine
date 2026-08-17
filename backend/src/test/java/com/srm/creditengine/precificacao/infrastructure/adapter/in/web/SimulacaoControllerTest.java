package com.srm.creditengine.precificacao.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.srm.creditengine.precificacao.application.PrecificacaoSimulator;
import com.srm.creditengine.precificacao.domain.ResultadoPrecificacao;
import com.srm.creditengine.precificacao.domain.Spread;
import com.srm.creditengine.precificacao.domain.exception.ExchangeRateUnavailableException;
import com.srm.creditengine.precificacao.domain.exception.UnknownCurrencyException;
import com.srm.creditengine.precificacao.domain.exception.UnknownReceivableTypeException;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import com.srm.creditengine.shared.domain.model.Dinheiro;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WithMockUser(roles = "ADMIN")
@WebMvcTest(SimulacaoController.class)
@AutoConfigureMockMvc(addFilters = false)
class SimulacaoControllerTest {

    private static final ResultadoPrecificacao RESULT = new ResultadoPrecificacao(
        new Dinheiro(new BigDecimal("985.22"), new CodigoMoeda("BRL"), 2),
        new Spread(new BigDecimal("0.015")), new BigDecimal("1.000000"),
        new Dinheiro(new BigDecimal("985.22"), new CodigoMoeda("BRL"), 2), null, null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PrecificacaoSimulator simulator;

    @Test
    void simulateReturns200() throws Exception {
        when(simulator.simulate(any(), any())).thenReturn(RESULT);

        mockMvc.perform(post("/api/simulacoes/precificacao")
                .contentType(APPLICATION_JSON)
                .content("{\"codigoTipo\":\"DUPLICATA_MERCANTIL\",\"valorFace\":1000.00," +
                    "\"codigoMoeda\":\"BRL\",\"dataVencimento\":\"2026-09-15\",\"codigoMoedaPagamento\":\"BRL\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valorPresente").value(985.22))
            .andExpect(jsonPath("$.codigoMoeda").value("BRL"))
            .andExpect(jsonPath("$.spreadAplicado").value(0.015))
            .andExpect(jsonPath("$.prazoMeses").value(1.000000))
            .andExpect(jsonPath("$.valorLiquido").value(985.22))
            .andExpect(jsonPath("$.codigoMoedaPagamento").value("BRL"))
            .andExpect(jsonPath("$.taxaAplicada").doesNotExist())
            .andExpect(jsonPath("$.vigenciaTaxa").doesNotExist());
    }

    @Test
    void simulateReturns400OnInvalidBody() throws Exception {
        mockMvc.perform(post("/api/simulacoes/precificacao")
                .contentType(APPLICATION_JSON)
                .content("{\"codigoTipo\":\"DUPLICATA_MERCANTIL\",\"valorFace\":-1," +
                    "\"codigoMoeda\":\"BRL\",\"dataVencimento\":\"2026-09-15\",\"codigoMoedaPagamento\":\"BRL\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void simulateReturns422OnUnknownTipo() throws Exception {
        when(simulator.simulate(any(), any())).thenThrow(new UnknownReceivableTypeException("X"));

        mockMvc.perform(post("/api/simulacoes/precificacao")
                .contentType(APPLICATION_JSON)
                .content("{\"codigoTipo\":\"X\",\"valorFace\":1000.00," +
                    "\"codigoMoeda\":\"BRL\",\"dataVencimento\":\"2026-09-15\",\"codigoMoedaPagamento\":\"BRL\"}"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void simulateReturns422OnUnknownCurrency() throws Exception {
        when(simulator.simulate(any(), any())).thenThrow(new UnknownCurrencyException("EUR"));

        mockMvc.perform(post("/api/simulacoes/precificacao")
                .contentType(APPLICATION_JSON)
                .content("{\"codigoTipo\":\"DUPLICATA_MERCANTIL\",\"valorFace\":1000.00," +
                    "\"codigoMoeda\":\"EUR\",\"dataVencimento\":\"2026-09-15\",\"codigoMoedaPagamento\":\"BRL\"}"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void simulateReturns422WhenNoExchangeRate() throws Exception {
        when(simulator.simulate(any(), any()))
            .thenThrow(new ExchangeRateUnavailableException(new CodigoMoeda("USD"), new CodigoMoeda("BRL")));

        mockMvc.perform(post("/api/simulacoes/precificacao")
                .contentType(APPLICATION_JSON)
                .content("{\"codigoTipo\":\"DUPLICATA_MERCANTIL\",\"valorFace\":1000.00," +
                    "\"codigoMoeda\":\"USD\",\"dataVencimento\":\"2026-09-15\",\"codigoMoedaPagamento\":\"BRL\"}"))
            .andExpect(status().isUnprocessableEntity());
    }
}