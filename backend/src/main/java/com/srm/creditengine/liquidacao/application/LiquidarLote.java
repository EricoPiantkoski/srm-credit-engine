package com.srm.creditengine.liquidacao.application;

import com.srm.creditengine.liquidacao.domain.ItemLiquidacao;
import com.srm.creditengine.liquidacao.domain.Liquidacao;
import com.srm.creditengine.liquidacao.domain.RepositorioLiquidacao;
import com.srm.creditengine.liquidacao.domain.StatusLiquidacao;
import com.srm.creditengine.liquidacao.domain.exception.LiquidacaoConflictException;
import com.srm.creditengine.liquidacao.domain.exception.LiquidacaoVersionConflictException;
import com.srm.creditengine.liquidacao.domain.exception.RecebivelNotFoundException;
import com.srm.creditengine.precificacao.application.PrecificacaoEngine;
import com.srm.creditengine.precificacao.domain.PrecificacaoStrategy;
import com.srm.creditengine.precificacao.domain.PrecificacaoStrategyResolver;
import com.srm.creditengine.precificacao.domain.Recebivel;
import com.srm.creditengine.precificacao.domain.RecebivelRepository;
import com.srm.creditengine.precificacao.domain.ResultadoPrecificacao;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;

public class LiquidarLote {

    private final RepositorioLiquidacao repositorioLiquidacao;
    private final RecebivelRepository recebivelRepository;
    private final PrecificacaoStrategyResolver strategyResolver;
    private final PrecificacaoEngine engine;

    public LiquidarLote(RepositorioLiquidacao repositorioLiquidacao,
                        RecebivelRepository recebivelRepository,
                        PrecificacaoStrategyResolver strategyResolver,
                        PrecificacaoEngine engine) {
        this.repositorioLiquidacao = repositorioLiquidacao;
        this.recebivelRepository = recebivelRepository;
        this.strategyResolver = strategyResolver;
        this.engine = engine;
    }

    @Transactional
    public Liquidacao liquidar(LiquidarLoteInput input) {
        Objects.requireNonNull(input, "input must not be null");
        if (repositorioLiquidacao.existsChaveIdempotencia(input.chaveIdempotencia())) {
            throw new LiquidacaoConflictException(input.chaveIdempotencia());
        }
        CodigoMoeda moedaPagamento = new CodigoMoeda(input.codigoMoedaPagamento());
        Instant reference = Instant.now();
        List<ItemLiquidacao> itens = input.recebiveisIds().stream()
            .map(id -> itemPara(id, moedaPagamento, reference))
            .toList();
        Liquidacao liquidacao = new Liquidacao(null, input.chaveIdempotencia(),
            StatusLiquidacao.LIQUIDADA, itens, reference);
        return repositorioLiquidacao.save(liquidacao);
    }

    private ItemLiquidacao itemPara(Long recebivelId, CodigoMoeda moedaPagamento, Instant reference) {
        Recebivel recebivel = recebivelRepository.obtainById(recebivelId)
            .orElseThrow(() -> new RecebivelNotFoundException(recebivelId));
        marcarRecebivelLiquidado(recebivel);
        PrecificacaoStrategy strategy = strategyResolver.resolveFor(recebivel.codigoTipo());
        ResultadoPrecificacao resultado = engine.price(recebivel, strategy, moedaPagamento, reference);
        return new ItemLiquidacao(
            recebivel.id(),
            resultado.valorPresente().valor(),
            resultado.spreadAplicado().valor(),
            resultado.prazoMeses(),
            resultado.valorLiquido().valor(),
            resultado.valorLiquido().moeda().codigo(),
            resultado.taxaAplicada());
    }

    private void marcarRecebivelLiquidado(Recebivel recebivel) {
        if (!recebivelRepository.marcarLiquidado(recebivel.id(), recebivel.version())) {
            throw new LiquidacaoVersionConflictException(recebivel.id(), recebivel.version());
        }
    }

    public record LiquidarLoteInput(String chaveIdempotencia, String codigoMoedaPagamento, List<Long> recebiveisIds) {
        private static final String UUID_V4_REGEX =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-4[0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$";

        public LiquidarLoteInput {
            Objects.requireNonNull(chaveIdempotencia, "chaveIdempotencia must not be null");
            Objects.requireNonNull(codigoMoedaPagamento, "codigoMoedaPagamento must not be null");
            Objects.requireNonNull(recebiveisIds, "recebiveisIds must not be null");
            if (!chaveIdempotencia.matches(UUID_V4_REGEX)) {
                throw new IllegalArgumentException("chaveIdempotencia must be UUID v4, but was: " + chaveIdempotencia);
            }
            if (recebiveisIds.isEmpty()) {
                throw new IllegalArgumentException("recebiveisIds must not be empty");
            }
        }
    }
}