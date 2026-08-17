package com.srm.creditengine.precificacao.domain.exception;

import com.srm.creditengine.shared.domain.exception.DomainException;

public class UnknownCurrencyException extends DomainException {
    public UnknownCurrencyException(String codigo) {
        super("currency not catalogued: " + codigo);
    }
}