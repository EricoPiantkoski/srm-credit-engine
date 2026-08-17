package com.srm.creditengine.precificacao.domain;

import com.srm.creditengine.precificacao.domain.exception.UnknownReceivableTypeException;

public class DuplicataMercantilStrategy implements PrecificacaoStrategy {

    private static final String CODIGO_TIPO = "DUPLICATA_MERCANTIL";

    private final TipoRecebivelRepository repository;

    public DuplicataMercantilStrategy(TipoRecebivelRepository repository) {
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