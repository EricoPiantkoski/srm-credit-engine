package com.srm.creditengine.precificacao.application;

import com.srm.creditengine.precificacao.domain.MoedaCatalog;
import com.srm.creditengine.precificacao.domain.PrecificacaoStrategy;
import com.srm.creditengine.precificacao.domain.PrecificacaoStrategyResolver;
import com.srm.creditengine.precificacao.domain.Recebivel;
import com.srm.creditengine.precificacao.domain.ResultadoPrecificacao;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import com.srm.creditengine.shared.domain.model.Dinheiro;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class PrecificacaoSimulator {

    private static final String SIMULATION_REFERENCIA_EXTERNA = "simulacao";
    private static final String SIMULATION_CEDENTE = "simulacao";

    private final PrecificacaoStrategyResolver resolver;
    private final PrecificacaoEngine engine;
    private final MoedaCatalog moedaCatalog;

    public PrecificacaoSimulator(PrecificacaoStrategyResolver resolver,
                                 PrecificacaoEngine engine, MoedaCatalog moedaCatalog) {
        this.resolver = resolver;
        this.engine = engine;
        this.moedaCatalog = moedaCatalog;
    }

    public ResultadoPrecificacao simulate(SimulatePrecificacaoInput input, Instant precificacaoReference) {
        int escala = moedaCatalog.scaleOf(new CodigoMoeda(input.codigoMoeda()));
        Recebivel recebivel = new Recebivel(null, SIMULATION_REFERENCIA_EXTERNA, input.codigoTipo(),
            new Dinheiro(input.valorFace(), new CodigoMoeda(input.codigoMoeda()), escala),
            input.dataVencimento(), SIMULATION_CEDENTE, 0L);
        PrecificacaoStrategy strategy = resolver.resolveFor(input.codigoTipo());
        return engine.price(recebivel, strategy, new CodigoMoeda(input.codigoMoedaPagamento()), precificacaoReference);
    }

    public record SimulatePrecificacaoInput(
        String codigoTipo, BigDecimal valorFace, String codigoMoeda,
        LocalDate dataVencimento, String codigoMoedaPagamento) {}
}