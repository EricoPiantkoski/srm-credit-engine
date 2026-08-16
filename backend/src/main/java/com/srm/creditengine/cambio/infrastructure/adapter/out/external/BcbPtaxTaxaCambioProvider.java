package com.srm.creditengine.cambio.infrastructure.adapter.out.external;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.cambio.domain.TaxaCambioProvider;
import com.srm.creditengine.cambio.domain.exception.ExchangeRateProviderUnavailableException;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import feign.FeignException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BcbPtaxTaxaCambioProvider implements TaxaCambioProvider {

    private static final String MOEDA_BRL = "BRL";
    private static final DateTimeFormatter PTAX_DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private final BcbPtaxClient client;
    private final String quoteCurrency;
    private final ZoneId timeZone;
    private final int retroactiveDays;

    public BcbPtaxTaxaCambioProvider(BcbPtaxClient client, String quoteCurrency, ZoneId timeZone, int retroactiveDays) {
        this.client = client;
        this.quoteCurrency = quoteCurrency;
        this.timeZone = timeZone;
        this.retroactiveDays = retroactiveDays;
    }

    @Override
    public Optional<TaxaCambio> obtain(ParMoedas par) {
        if (!supports(par)) {
            return Optional.empty();
        }
        LocalDate end = LocalDate.now(timeZone);
        LocalDate start = end.minusDays(retroactiveDays);
        Map<String, String> params = Map.of(
            "@moeda", "'" + quoteCurrency + "'",
            "@dataInicial", "'" + PTAX_DATE_FORMAT.format(start) + "'",
            "@dataFinalCotacao", "'" + PTAX_DATE_FORMAT.format(end) + "'",
            "$format", "json",
            "$select", "cotacaoVenda,dataHoraCotacao,tipoBoletim");
        PtaxResponse response;
        try {
            response = client.queryCotacao(params);
        } catch (FeignException ex) {
            throw new ExchangeRateProviderUnavailableException(par);
        }
        if (response == null || response.value() == null || response.value().isEmpty()) {
            return Optional.empty();
        }
        PtaxQuote quote = response.value().stream()
            .filter(c -> "Fechamento".equals(c.tipoBoletim()))
            .max(Comparator.comparing(PtaxQuote::dataHoraCotacao))
            .orElse(response.value().get(0));
        return Optional.of(new TaxaCambio(par, rateFor(par, quote), parseTimestamp(quote.dataHoraCotacao())));
    }

    private boolean supports(ParMoedas par) {
        return par.contem(new CodigoMoeda(MOEDA_BRL))
            && (par.base().codigo().equals(quoteCurrency) || par.cotacao().codigo().equals(quoteCurrency));
    }

    private BigDecimal rateFor(ParMoedas par, PtaxQuote quote) {
        BigDecimal sellRate = new BigDecimal(quote.cotacaoVenda());
        if (par.base().codigo().equals(quoteCurrency)) {
            return sellRate;
        }
        return BigDecimal.ONE.divide(sellRate, 8, RoundingMode.HALF_EVEN);
    }

    private Instant parseTimestamp(String value) {
        return LocalDateTime.parse(value.replace(' ', 'T')).atZone(timeZone).toInstant();
    }

    public record PtaxResponse(List<PtaxQuote> value) {}

    public record PtaxQuote(String cotacaoVenda, String dataHoraCotacao, String tipoBoletim) {}
}