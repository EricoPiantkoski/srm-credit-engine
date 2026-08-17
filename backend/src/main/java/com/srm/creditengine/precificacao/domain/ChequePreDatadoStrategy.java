package com.srm.creditengine.precificacao.domain;

import com.srm.creditengine.precificacao.domain.exception.UnknownReceivableTypeException;

public class ChequePreDatadoStrategy implements PrecificacaoStrategy {

    private static final String CODIGO_TIPO = "CHEQUE_PRE_DATADO";

    private final TipoRecebivelRepository repository;

    public ChequePreDatadoStrategy(TipoRecebivelRepository repository) {
        this.repository = repository;
    }

    @Override
    public Spread spreadFor(Recebivel recebivel) {
        return new Spread(resolveTipo().spread());
    }

    private TipoRecebivel resolveTipo() {
        return repository.obtainByCodigo(CODIGO_TIPO)
            .orElseThrow(() -> new UnknownReceivableTypeException(CODIGO_TIPO));
    }
}