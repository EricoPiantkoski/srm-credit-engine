package com.srm.creditengine.precificacao.infrastructure.adapter.in.web;

import com.srm.creditengine.precificacao.application.RecebivelCreator;
import com.srm.creditengine.precificacao.application.RecebivelQuery;
import com.srm.creditengine.precificacao.domain.Recebivel;
import com.srm.creditengine.precificacao.domain.RecebivelQueryCriteria;
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
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Recebíveis", description = "Cadastro e consulta de recebíveis")
@RequestMapping("/api/recebiveis")
public class RecebivelController {

    private final RecebivelCreator creator;
    private final RecebivelQuery query;

    public RecebivelController(RecebivelCreator creator, RecebivelQuery query) {
        this.creator = creator;
        this.query = query;
    }

    @PostMapping
    @Operation(summary = "Cria um recebível",
        description = "Registra um novo recebível para posterior precificação.")
    @ApiResponse(responseCode = "201", description = "Recebível criado")
    @ApiResponse(responseCode = "409", description = "Já existe recebível com a mesma referência externa")
    public ResponseEntity<RecebivelResponse> create(@Valid @RequestBody RecebivelCreateRequest request) {
        Recebivel recebivel = creator.create(new RecebivelCreator.CreateRecebivelInput(
            request.referenciaExterna(), request.codigoTipo(), request.valorFace(),
            request.codigoMoeda(), request.dataVencimento(), request.cedente()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(recebivel));
    }

    @GetMapping
    @Operation(summary = "Lista recebíveis",
        description = "Consulta recebíveis com filtros opcionais de cedente, moeda e tipo, com paginação.")
    @ApiResponse(responseCode = "200", description = "Lista de recebíveis")
    public ResponseEntity<List<RecebivelResponse>> list(
            @RequestParam(required = false) String cedente,
            @RequestParam(required = false) String codigoMoeda,
            @RequestParam(required = false) String codigoTipo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<RecebivelResponse> result = query.list(new RecebivelQueryCriteria(cedente, codigoMoeda, codigoTipo, page, size))
            .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(result);
    }

    private RecebivelResponse toResponse(Recebivel recebivel) {
        return new RecebivelResponse(
            recebivel.id(), recebivel.referenciaExterna(), recebivel.codigoTipo(),
            recebivel.valorFace().valor(), recebivel.valorFace().moeda().codigo(),
            recebivel.dataVencimento(), recebivel.cedente());
    }

    public record RecebivelCreateRequest(
        @NotBlank String referenciaExterna,
        @NotBlank String codigoTipo,
        @NotNull @Positive BigDecimal valorFace,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String codigoMoeda,
        @NotNull @Future LocalDate dataVencimento,
        @NotBlank String cedente) {}

    public record RecebivelResponse(
        Long id, String referenciaExterna, String codigoTipo, BigDecimal valorFace,
        String codigoMoeda, LocalDate dataVencimento, String cedente) {}
}