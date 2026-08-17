package com.srm.creditengine.liquidacao.application;

import com.srm.creditengine.liquidacao.domain.Liquidacao;
import com.srm.creditengine.liquidacao.domain.RepositorioLiquidacao;
import com.srm.creditengine.liquidacao.domain.exception.LiquidacaoNotFoundException;
import java.util.List;
import com.srm.creditengine.liquidacao.domain.RepositorioLiquidacao.PageResult;

public class ConsultarLiquidacao {

    private final RepositorioLiquidacao repositorioLiquidacao;

    public ConsultarLiquidacao(RepositorioLiquidacao repositorioLiquidacao) {
        this.repositorioLiquidacao = repositorioLiquidacao;
    }

    public Liquidacao obtainById(Long id) {
        return repositorioLiquidacao.obtainById(id)
            .orElseThrow(() -> new LiquidacaoNotFoundException(id));
    }

    public PageResult list(int page, int size) {
        return repositorioLiquidacao.list(page, size);
    }
}
