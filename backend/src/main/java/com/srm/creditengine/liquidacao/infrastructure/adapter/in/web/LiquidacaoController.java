package com.srm.creditengine.liquidacao.infrastructure.adapter.in.web;

import com.srm.creditengine.liquidacao.application.ConsultarLiquidacao;
import com.srm.creditengine.liquidacao.application.LiquidarLote;
import com.srm.creditengine.liquidacao.domain.ItemLiquidacao;
import com.srm.creditengine.liquidacao.domain.Liquidacao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Liquidações", description = "Liquidação de lotes de recebíveis")
@RequestMapping("/api/liquidacoes")
public class LiquidacaoController {

    private final LiquidarLote liquidarLote;
    private final ConsultarLiquidacao consultarLiquidacao;

    public LiquidacaoController(LiquidarLote liquidarLote, ConsultarLiquidacao consultarLiquidacao) {
        this.liquidarLote = liquidarLote;
        this.consultarLiquidacao = consultarLiquidacao;
    }

    @PostMapping
    @Operation(summary = "Liquida um lote de recebíveis",
        description = "Precifica e registra a liquidação de um lote em uma única transação atômica, "
            + "com controle de concorrência por versão e idempotência por chave.")
    @ApiResponse(responseCode = "201", description = "Liquidação criada")
    @ApiResponse(responseCode = "409", description = "Chave de idempotência já utilizada ou recebível modificado por outra transação")
    @ApiResponse(responseCode = "422", description = "Dados válidos porém não processáveis (ex.: recebível inexistente)")
    public ResponseEntity<LiquidacaoResponse> create(@Valid @RequestBody LiquidacaoCreateRequest request) {
        Liquidacao liquidacao = liquidarLote.liquidar(new LiquidarLote.LiquidarLoteInput(
            request.chaveIdempotencia(), request.codigoMoedaPagamento(), request.recebiveisIds()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(liquidacao));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta uma liquidação",
        description = "Retorna a liquidação com seus itens auditáveis.")
    @ApiResponse(responseCode = "200", description = "Liquidação encontrada")
    @ApiResponse(responseCode = "404", description = "Liquidação não encontrada")
    public ResponseEntity<LiquidacaoResponse> obtain(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(consultarLiquidacao.obtainById(id)));
    }

    private LiquidacaoResponse toResponse(Liquidacao liquidacao) {
        List<ItemLiquidacaoResponse> itens = liquidacao.itens().stream().map(this::toItemResponse).toList();
        return new LiquidacaoResponse(
            liquidacao.id(), liquidacao.chaveIdempotencia(), liquidacao.status().name(),
            liquidacao.createdAt(), itens);
    }

    private ItemLiquidacaoResponse toItemResponse(ItemLiquidacao item) {
        return new ItemLiquidacaoResponse(
            item.recebivelId(), item.valorPresente(), item.spreadAplicado(), item.prazoMeses(),
            item.valorPagamento(), item.codigoMoedaPagamento(), item.taxaAplicada());
    }

    public record LiquidacaoCreateRequest(
        @NotBlank
        @Pattern(regexp = UUID_REGEX, message = "chaveIdempotencia deve ser um UUID")
        @Size(max = 36)
        String chaveIdempotencia,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String codigoMoedaPagamento,
        @NotEmpty List<@NotNull Long> recebiveisIds) {}

    private static final String UUID_REGEX =
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    public record ItemLiquidacaoResponse(
        Long recebivelId, BigDecimal valorPresente, BigDecimal spreadAplicado,
        BigDecimal prazoMeses, BigDecimal valorPagamento, String codigoMoedaPagamento,
        BigDecimal taxaAplicada) {}

    public record LiquidacaoResponse(
        Long id, String chaveIdempotencia, String status, Instant createdAt,
        List<ItemLiquidacaoResponse> itens) {}
}