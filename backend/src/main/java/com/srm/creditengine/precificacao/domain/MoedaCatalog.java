package com.srm.creditengine.precificacao.domain;

import com.srm.creditengine.shared.domain.model.CodigoMoeda;

public interface MoedaCatalog {

    int scaleOf(CodigoMoeda moeda);
}