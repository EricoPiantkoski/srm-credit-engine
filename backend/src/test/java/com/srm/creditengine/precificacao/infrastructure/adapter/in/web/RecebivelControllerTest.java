package com.srm.creditengine.precificacao.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.srm.creditengine.precificacao.application.RecebivelCreator;
import com.srm.creditengine.precificacao.application.RecebivelQuery;
import com.srm.creditengine.precificacao.domain.Recebivel;
import com.srm.creditengine.precificacao.domain.exception.ReceivableConflictException;
import com.srm.creditengine.precificacao.domain.exception.UnknownReceivableTypeException;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import com.srm.creditengine.shared.domain.model.Dinheiro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WithMockUser(roles = "ADMIN")
@WebMvcTest(RecebivelController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecebivelControllerTest {

    private static final Recebivel RECEBIVEL = new Recebivel(
        1L, "REF-001", "DUPLICATA_MERCANTIL",
        new Dinheiro(new BigDecimal("1000.00"), new CodigoMoeda("BRL"), 2),
        LocalDate.of(2026, 9, 15), "Cedente", 0L);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecebivelCreator creator;

    @MockitoBean
    private RecebivelQuery query;

    @Test
    void createReturns201() throws Exception {
        when(creator.create(any())).thenReturn(RECEBIVEL);

        mockMvc.perform(post("/api/recebiveis")
                .contentType(APPLICATION_JSON)
                .content("{\"referenciaExterna\":\"REF-001\",\"codigoTipo\":\"DUPLICATA_MERCANTIL\"," +
                    "\"valorFace\":1000.00,\"codigoMoeda\":\"BRL\",\"dataVencimento\":\"2026-09-15\",\"cedente\":\"Cedente\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.referenciaExterna").value("REF-001"))
            .andExpect(jsonPath("$.codigoTipo").value("DUPLICATA_MERCANTIL"))
            .andExpect(jsonPath("$.valorFace").value(1000.00))
            .andExpect(jsonPath("$.codigoMoeda").value("BRL"))
            .andExpect(jsonPath("$.dataVencimento").value("2026-09-15"))
            .andExpect(jsonPath("$.cedente").value("Cedente"))
            .andExpect(jsonPath("$.status").value("DISPONIVEL"));
    }

    @Test
    void createReturns400OnInvalidBody() throws Exception {
        mockMvc.perform(post("/api/recebiveis")
                .contentType(APPLICATION_JSON)
                .content("{\"referenciaExterna\":\"REF-001\",\"codigoTipo\":\"DUPLICATA_MERCANTIL\"," +
                    "\"valorFace\":-1,\"codigoMoeda\":\"BRL\",\"dataVencimento\":\"2026-09-15\",\"cedente\":\"Cedente\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createReturns400OnPastVencimento() throws Exception {
        mockMvc.perform(post("/api/recebiveis")
                .contentType(APPLICATION_JSON)
                .content("{\"referenciaExterna\":\"REF-001\",\"codigoTipo\":\"DUPLICATA_MERCANTIL\"," +
                    "\"valorFace\":1000.00,\"codigoMoeda\":\"BRL\",\"dataVencimento\":\"2020-01-01\",\"cedente\":\"Cedente\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createReturns409OnConflict() throws Exception {
        when(creator.create(any())).thenThrow(new ReceivableConflictException("REF-001"));

        mockMvc.perform(post("/api/recebiveis")
                .contentType(APPLICATION_JSON)
                .content("{\"referenciaExterna\":\"REF-001\",\"codigoTipo\":\"DUPLICATA_MERCANTIL\"," +
                    "\"valorFace\":1000.00,\"codigoMoeda\":\"BRL\",\"dataVencimento\":\"2026-09-15\",\"cedente\":\"Cedente\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void listReturns200() throws Exception {
        when(query.list(any())).thenReturn(List.of(RECEBIVEL));

        mockMvc.perform(get("/api/recebiveis"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].referenciaExterna").value("REF-001"));
    }
}