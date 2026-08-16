package com.srm.creditengine.shared.domain.exception;

import com.srm.creditengine.shared.domain.model.CodigoMoeda;

public class IncompatibleCurrenciesException extends DomainException {

    public IncompatibleCurrenciesException(CodigoMoeda first, CodigoMoeda second) {
        super("Cannot operate on amounts of different currencies: "
            + first + " and " + second);
    }
}