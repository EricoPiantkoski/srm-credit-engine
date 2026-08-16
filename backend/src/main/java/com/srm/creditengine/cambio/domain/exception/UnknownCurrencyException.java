package com.srm.creditengine.cambio.domain.exception;

import com.srm.creditengine.shared.domain.exception.DomainException;

public class UnknownCurrencyException extends DomainException {
    public UnknownCurrencyException(String codigo) {
        super("currency code does not exist: " + codigo);
    }
}