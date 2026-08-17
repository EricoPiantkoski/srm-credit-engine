package com.srm.creditengine.precificacao.domain;

import com.srm.creditengine.precificacao.domain.exception.UnknownReceivableTypeException;
import java.util.Map;

public class PrecificacaoStrategyResolver {

    private final Map<String, PrecificacaoStrategy> strategies;

    public PrecificacaoStrategyResolver(Map<String, PrecificacaoStrategy> strategies) {
        this.strategies = Map.copyOf(strategies);
    }

    public PrecificacaoStrategy resolveFor(String codigoTipo) {
        PrecificacaoStrategy strategy = strategies.get(codigoTipo);
        if (strategy == null) {
            throw new UnknownReceivableTypeException(codigoTipo);
        }
        return strategy;
    }
}