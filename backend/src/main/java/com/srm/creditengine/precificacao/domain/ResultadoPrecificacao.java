package com.srm.creditengine.precificacao.domain;

import com.srm.creditengine.shared.domain.model.Dinheiro;
import java.math.BigDecimal;
import java.time.Instant;

public record ResultadoPrecificacao(
    Dinheiro valorPresente,
    Spread spreadAplicado,
    BigDecimal prazoMeses,
    Dinheiro valorLiquido,
    BigDecimal taxaAplicada,
    Instant vigenciaTaxa) {}