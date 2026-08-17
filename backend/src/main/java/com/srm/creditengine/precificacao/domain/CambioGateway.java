package com.srm.creditengine.precificacao.domain;

import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import com.srm.creditengine.shared.domain.model.Dinheiro;
import java.time.Instant;

public interface CambioGateway {

    TaxaCambioAplicada convert(Dinheiro valor, CodigoMoeda moedaPagamento, Instant precificacaoReference);
}