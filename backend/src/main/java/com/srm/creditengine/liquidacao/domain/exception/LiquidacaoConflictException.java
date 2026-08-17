package com.srm.creditengine.liquidacao.domain.exception;

import com.srm.creditengine.shared.domain.exception.DomainException;

public class LiquidacaoConflictException extends DomainException {
    public LiquidacaoConflictException(String chaveIdempotencia) {
        super("liquidação already exists with chaveIdempotencia: " + chaveIdempotencia);
    }
}