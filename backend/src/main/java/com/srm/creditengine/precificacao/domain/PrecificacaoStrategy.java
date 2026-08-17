package com.srm.creditengine.precificacao.domain;

public interface PrecificacaoStrategy {
    Spread spreadFor(Recebivel recebivel);
}