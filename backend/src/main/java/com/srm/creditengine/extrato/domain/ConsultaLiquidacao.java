package com.srm.creditengine.extrato.domain;

import java.util.List;

public interface ConsultaLiquidacao {

    List<ExtratoLiquidacao> consultar(ExtratoFiltros filtros);
}