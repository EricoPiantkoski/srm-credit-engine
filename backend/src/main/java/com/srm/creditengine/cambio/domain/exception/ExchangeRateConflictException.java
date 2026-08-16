package com.srm.creditengine.cambio.domain.exception;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.shared.domain.exception.DomainException;
import java.time.Instant;

public class ExchangeRateConflictException extends DomainException {
    public ExchangeRateConflictException(ParMoedas par, Instant vigencia) {
        super("exchange rate already exists for pair " + par + " at " + vigencia);
    }
}