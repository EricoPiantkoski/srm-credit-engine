package com.srm.creditengine.liquidacao.domain.exception;

import com.srm.creditengine.shared.domain.exception.DomainException;

public class LiquidacaoNotFoundException extends DomainException {
    public LiquidacaoNotFoundException(Long id) {
        super("liquidação not found with id: " + id);
    }
}