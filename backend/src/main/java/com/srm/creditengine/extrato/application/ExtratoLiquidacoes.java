package com.srm.creditengine.extrato.application;

import com.srm.creditengine.extrato.domain.ConsultaLiquidacao;
import com.srm.creditengine.extrato.domain.ExtratoFiltros;
import com.srm.creditengine.extrato.domain.ExtratoLiquidacao;
import java.util.List;

public class ExtratoLiquidacoes {

    private final ConsultaLiquidacao consultaLiquidacao;

    public ExtratoLiquidacoes(ConsultaLiquidacao consultaLiquidacao) {
        this.consultaLiquidacao = consultaLiquidacao;
    }

    public List<ExtratoLiquidacao> consultar(ExtratoFiltros filtros) {
        return consultaLiquidacao.consultar(filtros);
    }
}