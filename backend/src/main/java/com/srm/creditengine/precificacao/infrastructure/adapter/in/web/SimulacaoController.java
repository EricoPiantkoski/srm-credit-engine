package com.srm.creditengine.precificacao.infrastructure.adapter.in.web;

import com.srm.creditengine.precificacao.application.PrecificacaoSimulator;
import com.srm.creditengine.precificacao.domain.ResultadoPrecificacao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Simulações de Precificação", description = "Simulação de precificação de ativos")
@RequestMapping("/api/simulacoes")
public class SimulacaoController {

    private final PrecificacaoSimulator simulator;

    public SimulacaoController(PrecificacaoSimulator simulator) {
        this.simulator = simulator;
    }

    @PostMapping("/precificacao")
    @Operation(summary = "Simula a precificação de um ativo",
        description = "Calcula valor presente e valor líquido aplicando spread e taxa de câmbio, "
            + "se a moeda de pagamento for diferente da moeda do ativo.")
    @ApiResponse(responseCode = "200", description = "Precificação calculada")
    @ApiResponse(responseCode = "422", description = "Dados inválidos ou taxa de câmbio indisponível")
    @ApiResponse(responseCode = "503", description = "Provedor de câmbio externo indisponível")
    public ResponseEntity<PrecificacaoResponse> simulate(@Valid @RequestBody PrecificacaoRequest request) {
        ResultadoPrecificacao result = simulator.simulate(
            new PrecificacaoSimulator.SimulatePrecificacaoInput(
                request.codigoTipo(), request.valorFace(), request.codigoMoeda(),
                request.dataVencimento(), request.codigoMoedaPagamento()),
            Instant.now());
        return ResponseEntity.ok(toResponse(result));
    }

    private PrecificacaoResponse toResponse(ResultadoPrecificacao result) {
        return new PrecificacaoResponse(
            result.valorPresente().valor(), result.valorPresente().moeda().codigo(),
            result.spreadAplicado().valor(), result.prazoMeses(),
            result.valorLiquido().valor(), result.valorLiquido().moeda().codigo(),
            result.taxaAplicada(), result.vigenciaTaxa());
    }

    public record PrecificacaoRequest(
        @NotBlank String codigoTipo,
        @NotNull @Positive BigDecimal valorFace,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String codigoMoeda,
        @NotNull @Future LocalDate dataVencimento,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String codigoMoedaPagamento) {}

    public record PrecificacaoResponse(
        BigDecimal valorPresente, String codigoMoeda,
        BigDecimal spreadAplicado, BigDecimal prazoMeses,
        BigDecimal valorLiquido, String codigoMoedaPagamento,
        BigDecimal taxaAplicada, Instant vigenciaTaxa) {}
}