package com.srm.creditengine.cambio.domain.exception;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.shared.domain.exception.DomainException;

public class ExchangeRateProviderUnavailableException extends DomainException {
    public ExchangeRateProviderUnavailableException(ParMoedas par) {
        super("external exchange rate provider unavailable for pair " + par);
    }
}