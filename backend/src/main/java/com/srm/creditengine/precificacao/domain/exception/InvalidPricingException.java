package com.srm.creditengine.precificacao.domain.exception;

import com.srm.creditengine.shared.domain.exception.DomainException;

public class InvalidPricingException extends DomainException {
    public InvalidPricingException(String message) {
        super(message);
    }
}