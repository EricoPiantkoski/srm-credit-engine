package com.srm.creditengine.cambio.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.srm.creditengine.cambio.application.DinheiroConverter;
import com.srm.creditengine.cambio.application.TaxaCambioOrchestrator;
import com.srm.creditengine.cambio.application.TaxaCambioUpdater;
import com.srm.creditengine.cambio.application.TaxaVigenteReader;
import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.cambio.domain.exception.ExchangeRateConflictException;
import com.srm.creditengine.cambio.domain.exception.ExchangeRateProviderUnavailableException;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import com.srm.creditengine.shared.domain.model.Dinheiro;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TaxaCambioController.class)
class TaxaCambioControllerTest {

    private static final ParMoedas USD_BRL = new ParMoedas(new CodigoMoeda("USD"), new CodigoMoeda("BRL"));
    private static final TaxaCambio TAXA =
        new TaxaCambio(USD_BRL, new BigDecimal("5.25"), Instant.parse("2026-08-14T16:00:00Z"));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaxaCambioUpdater taxaCambioUpdater;

    @MockitoBean
    private TaxaVigenteReader taxaVigenteReader;

    @MockitoBean
    private DinheiroConverter dinheiroConverter;

    @MockitoBean
    private TaxaCambioOrchestrator taxaCambioOrchestrator;

    @Test
    void updateReturns200() throws Exception {
        when(taxaCambioUpdater.update(any(), any(), any())).thenReturn(TAXA);

        mockMvc.perform(put("/api/taxas-cambio")
                .contentType(APPLICATION_JSON)
                .content("{\"codigoBase\":\"USD\",\"codigoCotacao\":\"BRL\",\"taxa\":5.25,\"vigencia\":\"2026-08-14T16:00:00Z\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.codigoBase").value("USD"))
            .andExpect(jsonPath("$.codigoCotacao").value("BRL"))
            .andExpect(jsonPath("$.taxa").value(5.25));
    }

    @Test
    void updateReturns409OnConflict() throws Exception {
        when(taxaCambioUpdater.update(any(), any(), any()))
            .thenThrow(new ExchangeRateConflictException(USD_BRL, Instant.parse("2026-08-14T16:00:00Z")));

        mockMvc.perform(put("/api/taxas-cambio")
                .contentType(APPLICATION_JSON)
                .content("{\"codigoBase\":\"USD\",\"codigoCotacao\":\"BRL\",\"taxa\":5.25,\"vigencia\":\"2026-08-14T16:00:00Z\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void updateReturns400OnInvalidBody() throws Exception {
        mockMvc.perform(put("/api/taxas-cambio")
                .contentType(APPLICATION_JSON)
                .content("{\"codigoBase\":\"usd\",\"codigoCotacao\":\"BRL\",\"taxa\":-1,\"vigencia\":\"2026-08-14T16:00:00Z\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void currentReturns200() throws Exception {
        when(taxaVigenteReader.readOrObtain(any(), any())).thenReturn(Optional.of(TAXA));

        mockMvc.perform(get("/api/taxas-cambio/vigente")
                .param("codigoBase", "USD")
                .param("codigoCotacao", "BRL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.codigoBase").value("USD"))
            .andExpect(jsonPath("$.codigoCotacao").value("BRL"))
            .andExpect(jsonPath("$.taxa").value(5.25));
    }

    @Test
    void currentReturns404WhenMissing() throws Exception {
        when(taxaVigenteReader.readOrObtain(any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/taxas-cambio/vigente")
                .param("codigoBase", "USD")
                .param("codigoCotacao", "BRL"))
            .andExpect(status().isNotFound());
    }

    @Test
    void currentReturns400OnInvalidParam() throws Exception {
        mockMvc.perform(get("/api/taxas-cambio/vigente")
                .param("codigoBase", "usd")
                .param("codigoCotacao", "BRL"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void orchestrateReturns200() throws Exception {
        when(taxaCambioOrchestrator.orchestrate(any())).thenReturn(TAXA);

        mockMvc.perform(post("/api/taxas-cambio/integracao")
                .param("codigoBase", "USD")
                .param("codigoCotacao", "BRL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.taxa").value(5.25));
    }

    @Test
    void orchestrateReturns503WhenProviderUnavailable() throws Exception {
        when(taxaCambioOrchestrator.orchestrate(any()))
            .thenThrow(new ExchangeRateProviderUnavailableException(USD_BRL));

        mockMvc.perform(post("/api/taxas-cambio/integracao")
                .param("codigoBase", "USD")
                .param("codigoCotacao", "BRL"))
            .andExpect(status().isServiceUnavailable());
    }

    @Test
    void convertReturns200() throws Exception {
        when(taxaVigenteReader.read(any(), any())).thenReturn(Optional.of(TAXA));
        when(dinheiroConverter.convert(any(), any()))
            .thenReturn(new Dinheiro(new BigDecimal("2625.00"), new CodigoMoeda("BRL"), 2));

        mockMvc.perform(post("/api/taxas-cambio/convert")
                .contentType(APPLICATION_JSON)
                .content("{\"valor\":500.00,\"codigoMoeda\":\"USD\",\"escala\":2,\"codigoBase\":\"USD\",\"codigoCotacao\":\"BRL\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valor").value(2625.00))
            .andExpect(jsonPath("$.codigoMoeda").value("BRL"))
            .andExpect(jsonPath("$.appliedTaxa").value(5.25));
    }

    @Test
    void convertReturns503WhenFallbackProviderUnavailable() throws Exception {
        when(taxaVigenteReader.read(any(), any())).thenReturn(Optional.empty());
        when(taxaCambioOrchestrator.orchestrate(any()))
            .thenThrow(new ExchangeRateProviderUnavailableException(USD_BRL));

        mockMvc.perform(post("/api/taxas-cambio/convert")
                .contentType(APPLICATION_JSON)
                .content("{\"valor\":500.00,\"codigoMoeda\":\"USD\",\"escala\":2,\"codigoBase\":\"USD\",\"codigoCotacao\":\"BRL\"}"))
            .andExpect(status().isServiceUnavailable());
    }

    @Test
    void convertFallsBackToOrchestratorWhenNoTaxa() throws Exception {
        when(taxaVigenteReader.read(any(), any())).thenReturn(Optional.empty());
        when(taxaCambioOrchestrator.orchestrate(any())).thenReturn(TAXA);
        when(dinheiroConverter.convert(any(), any()))
            .thenReturn(new Dinheiro(new BigDecimal("2625.00"), new CodigoMoeda("BRL"), 2));

        mockMvc.perform(post("/api/taxas-cambio/convert")
                .contentType(APPLICATION_JSON)
                .content("{\"valor\":500.00,\"codigoMoeda\":\"USD\",\"escala\":2,\"codigoBase\":\"USD\",\"codigoCotacao\":\"BRL\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valor").value(2625.00))
            .andExpect(jsonPath("$.appliedTaxa").value(5.25));
    }

    @Test
    void convertReturns400OnInvalidBody() throws Exception {
        mockMvc.perform(post("/api/taxas-cambio/convert")
                .contentType(APPLICATION_JSON)
                .content("{\"valor\":-1,\"codigoMoeda\":\"USD\",\"escala\":2,\"codigoBase\":\"USD\",\"codigoCotacao\":\"BRL\"}"))
            .andExpect(status().isBadRequest());
    }
}