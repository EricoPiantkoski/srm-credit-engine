package com.srm.creditengine.precificacao.domain.exception;

import com.srm.creditengine.shared.domain.exception.DomainException;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;

public class ExchangeRateUnavailableException extends DomainException {
    public ExchangeRateUnavailableException(CodigoMoeda base, CodigoMoeda cotacao) {
        super("no exchange rate available to price cross-currency " + base + "/" + cotacao);
    }
}