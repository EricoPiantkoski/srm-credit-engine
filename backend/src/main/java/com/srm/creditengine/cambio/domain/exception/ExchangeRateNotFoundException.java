package com.srm.creditengine.cambio.domain.exception;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.shared.domain.exception.DomainException;

public class ExchangeRateNotFoundException extends DomainException {
    public ExchangeRateNotFoundException(ParMoedas par) {
        super("no exchange rate found for pair " + par);
    }
}