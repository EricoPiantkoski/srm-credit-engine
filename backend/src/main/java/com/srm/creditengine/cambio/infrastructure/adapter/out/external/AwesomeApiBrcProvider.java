package com.srm.creditengine.cambio.infrastructure.adapter.out.external;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.cambio.domain.TaxaCambioProvider;
import com.srm.creditengine.cambio.domain.exception.ExchangeRateProviderUnavailableException;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class AwesomeApiBrcProvider implements TaxaCambioProvider {

    private static final CodigoMoeda MOEDA_BRL = new CodigoMoeda("BRL");
    private static final CodigoMoeda MOEDA_USD = new CodigoMoeda("USD");
    private static final int SCALE = 8;
    private static final DateTimeFormatter CREATE_DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AwesomeApiBrcClient client;
    private final ZoneId timeZone;

    public AwesomeApiBrcProvider(AwesomeApiBrcClient client, ZoneId timeZone) {
        this.client = client;
        this.timeZone = timeZone;
    }

    @Override
    @CircuitBreaker(name = "awesomeApiBrc", fallbackMethod = "obtainFallback")
    public Optional<TaxaCambio> obtain(ParMoedas par) {
        if (!supports(par)) {
            return Optional.empty();
        }
        AwesomeApiBrcClient.AwesomeApiBrcResponse response;
        try {
            response = client.lastBrcUsd();
        } catch (FeignException ex) {
            throw new ExchangeRateProviderUnavailableException(par);
        }
        if (response == null || response.quote() == null || response.quote().ask() == null) {
            return Optional.empty();
        }
        BigDecimal ask = new BigDecimal(response.quote().ask()).setScale(SCALE, RoundingMode.HALF_EVEN);
        Instant vigencia = parseTimestamp(response.quote().createDate());
        return Optional.of(new TaxaCambio(par, ask, vigencia));
    }

    public boolean supports(ParMoedas par) {
        return MOEDA_BRL.equals(par.base()) && MOEDA_USD.equals(par.cotacao());
    }

    private Instant parseTimestamp(String value) {
        return LocalDateTime.parse(value, CREATE_DATE_FORMAT).atZone(timeZone).toInstant();
    }

    private Optional<TaxaCambio> obtainFallback(ParMoedas par, Throwable t) {
        throw new ExchangeRateProviderUnavailableException(par);
    }
}