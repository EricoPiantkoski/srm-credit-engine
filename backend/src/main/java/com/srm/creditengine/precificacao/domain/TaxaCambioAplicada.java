package com.srm.creditengine.precificacao.domain;

import com.srm.creditengine.shared.domain.model.Dinheiro;
import java.math.BigDecimal;
import java.time.Instant;

public record TaxaCambioAplicada(Dinheiro valor, BigDecimal taxa, Instant vigencia) {}