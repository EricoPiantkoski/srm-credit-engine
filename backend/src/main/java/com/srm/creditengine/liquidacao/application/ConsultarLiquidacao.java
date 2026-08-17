package com.srm.creditengine.liquidacao.application;

import com.srm.creditengine.liquidacao.domain.Liquidacao;
import com.srm.creditengine.liquidacao.domain.RepositorioLiquidacao;
import com.srm.creditengine.liquidacao.domain.exception.LiquidacaoNotFoundException;

public class ConsultarLiquidacao {

    private final RepositorioLiquidacao repositorioLiquidacao;

    public ConsultarLiquidacao(RepositorioLiquidacao repositorioLiquidacao) {
        this.repositorioLiquidacao = repositorioLiquidacao;
    }

    public Liquidacao obtainById(Long id) {
        return repositorioLiquidacao.obtainById(id)
            .orElseThrow(() -> new LiquidacaoNotFoundException(id));
    }
}