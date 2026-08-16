package com.srm.creditengine.cambio.infrastructure.adapter.in.web;

import com.srm.creditengine.cambio.application.DinheiroConverter;
import com.srm.creditengine.cambio.application.TaxaCambioOrchestrator;
import com.srm.creditengine.cambio.application.TaxaCambioUpdater;
import com.srm.creditengine.cambio.application.TaxaVigenteReader;
import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.cambio.domain.exception.ExchangeRateNotFoundException;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import com.srm.creditengine.shared.domain.model.Dinheiro;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/taxas-cambio")
public class TaxaCambioController {

    private final TaxaCambioUpdater taxaCambioUpdater;
    private final TaxaVigenteReader taxaVigenteReader;
    private final DinheiroConverter dinheiroConverter;
    private final TaxaCambioOrchestrator taxaCambioOrchestrator;

    public TaxaCambioController(TaxaCambioUpdater taxaCambioUpdater, TaxaVigenteReader taxaVigenteReader,
                                DinheiroConverter dinheiroConverter, TaxaCambioOrchestrator taxaCambioOrchestrator) {
        this.taxaCambioUpdater = taxaCambioUpdater;
        this.taxaVigenteReader = taxaVigenteReader;
        this.dinheiroConverter = dinheiroConverter;
        this.taxaCambioOrchestrator = taxaCambioOrchestrator;
    }

    @PutMapping
    public ResponseEntity<TaxaCambioResponse> update(@Valid @RequestBody TaxaCambioUpdateRequest request) {
        ParMoedas par = new ParMoedas(new CodigoMoeda(request.codigoBase()), new CodigoMoeda(request.codigoCotacao()));
        TaxaCambio taxa = taxaCambioUpdater.update(par, request.taxa(), request.vigencia());
        return ResponseEntity.ok(toResponse(taxa));
    }

    @GetMapping("/vigente")
    public ResponseEntity<TaxaCambioResponse> current(
            @RequestParam @Pattern(regexp = "[A-Z]{3}") String codigoBase,
            @RequestParam @Pattern(regexp = "[A-Z]{3}") String codigoCotacao) {
        ParMoedas par = new ParMoedas(new CodigoMoeda(codigoBase), new CodigoMoeda(codigoCotacao));
        return taxaVigenteReader.readOrObtain(par, Instant.now())
            .map(taxa -> ResponseEntity.ok(toResponse(taxa)))
            .orElseThrow(() -> new ExchangeRateNotFoundException(par));
    }

    @PostMapping("/integracao")
    public ResponseEntity<TaxaCambioResponse> orchestrate(
            @RequestParam @Pattern(regexp = "[A-Z]{3}") String codigoBase,
            @RequestParam @Pattern(regexp = "[A-Z]{3}") String codigoCotacao) {
        ParMoedas par = new ParMoedas(new CodigoMoeda(codigoBase), new CodigoMoeda(codigoCotacao));
        TaxaCambio taxa = taxaCambioOrchestrator.orchestrate(par);
        return ResponseEntity.ok(toResponse(taxa));
    }

    @PostMapping("/convert")
    public ResponseEntity<DinheiroConverterResponse> convert(@Valid @RequestBody DinheiroConverterRequest request) {
        Dinheiro valor = new Dinheiro(request.valor(), new CodigoMoeda(request.codigoMoeda()), request.escala());
        ParMoedas par = new ParMoedas(new CodigoMoeda(request.codigoBase()), new CodigoMoeda(request.codigoCotacao()));
        TaxaCambio taxa = taxaVigenteReader.read(par, Instant.now())
            .orElseGet(() -> taxaCambioOrchestrator.orchestrate(par));
        Dinheiro converted = dinheiroConverter.convert(valor, taxa);
        return ResponseEntity.ok(new DinheiroConverterResponse(
            converted.valor(), converted.moeda().codigo(), taxa.taxa(), taxa.vigencia()));
    }

    private TaxaCambioResponse toResponse(TaxaCambio taxa) {
        return new TaxaCambioResponse(
            taxa.par().base().codigo(), taxa.par().cotacao().codigo(), taxa.taxa(), taxa.vigencia());
    }

    public record TaxaCambioUpdateRequest(
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String codigoBase,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String codigoCotacao,
        @NotNull @Positive BigDecimal taxa,
        @NotNull Instant vigencia) {}

    public record DinheiroConverterRequest(
        @NotNull @Positive BigDecimal valor,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String codigoMoeda,
        int escala,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String codigoBase,
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String codigoCotacao) {}

    public record TaxaCambioResponse(String codigoBase, String codigoCotacao, BigDecimal taxa, Instant vigencia) {}

    public record DinheiroConverterResponse(BigDecimal valor, String codigoMoeda, BigDecimal appliedTaxa, Instant vigencia) {}
}