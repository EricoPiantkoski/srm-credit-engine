package com.srm.creditengine.extrato.infrastructure.adapter.in.web;

import com.srm.creditengine.extrato.application.ExtratoLiquidacoes;
import com.srm.creditengine.extrato.domain.ExtratoFiltros;
import com.srm.creditengine.extrato.domain.ExtratoLiquidacao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.constraints.Max;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Tag(name = "Extrato de Liquidações", description = "Consulta analítica de liquidações")
@RequestMapping("/api/liquidacoes")
public class ExtratoController {

    private final ExtratoLiquidacoes extratoLiquidacoes;

    public ExtratoController(ExtratoLiquidacoes extratoLiquidacoes) {
        this.extratoLiquidacoes = extratoLiquidacoes;
    }

    @GetMapping("/extrato")
    @Operation(summary = "Consulta o extrato de liquidações",
        description = "Lista itens de liquidações com filtros por período, status, cedente e moeda de pagamento, "
            + "com paginação por cursor.")
    @ApiResponse(responseCode = "200", description = "Extrato consultado")
    @ApiResponse(responseCode = "400", description = "Parâmetros inválidos")
    public ResponseEntity<List<ExtratoLiquidacaoResponse>> extrato(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicial,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFinal,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String cedente,
            @RequestParam(required = false) String codigoMoedaPagamento,
            @RequestParam(required = false) Long lastId,
            @RequestParam(defaultValue = "20") @Max(ExtratoFiltros.MAX_LIMIT) int limit) {
        List<ExtratoLiquidacao> result = extratoLiquidacoes.consultar(new ExtratoFiltros(
            dataInicial != null ? dataInicial : LocalDate.of(1970, 1, 1),
            dataFinal != null ? dataFinal : LocalDate.of(9999, 12, 31),
            status, cedente, codigoMoedaPagamento, lastId, limit));
        return ResponseEntity.ok(result.stream().map(this::toResponse).toList());
    }

    private ExtratoLiquidacaoResponse toResponse(ExtratoLiquidacao extrato) {
        return new ExtratoLiquidacaoResponse(
            extrato.itemId(), extrato.liquidacaoId(), extrato.chaveIdempotencia(), extrato.status(),
            extrato.createdAt(), extrato.recebivelId(), extrato.cedente(), extrato.valorPresente(),
            extrato.spreadAplicado(), extrato.prazoMeses(), extrato.valorPagamento(),
            extrato.codigoMoedaPagamento(), extrato.taxaAplicada());
    }

    public record ExtratoLiquidacaoResponse(
        Long itemId, Long liquidacaoId, String chaveIdempotencia, String status, Instant createdAt,
        Long recebivelId, String cedente, BigDecimal valorPresente, BigDecimal spreadAplicado,
        BigDecimal prazoMeses, BigDecimal valorPagamento, String codigoMoedaPagamento, BigDecimal taxaAplicada) {}
}
