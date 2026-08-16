package com.srm.creditengine.cambio.infrastructure.adapter.out.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.cambio.domain.exception.ExchangeRateProviderUnavailableException;
import com.srm.creditengine.cambio.infrastructure.adapter.out.external.BcbPtaxTaxaCambioProvider.PtaxQuote;
import com.srm.creditengine.cambio.infrastructure.adapter.out.external.BcbPtaxTaxaCambioProvider.PtaxResponse;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import feign.Request;

@ExtendWith(MockitoExtension.class)
class BcbPtaxTaxaCambioProviderTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    @Mock
    private BcbPtaxClient client;

    private BcbPtaxTaxaCambioProvider provider;

    @BeforeEach
    void setUp() {
        provider = new BcbPtaxTaxaCambioProvider(client, "USD", SAO_PAULO, 3);
    }

    private PtaxResponse response(String venda, String dataHora, String boletim) {
        return new PtaxResponse(List.of(new PtaxQuote(venda, dataHora, boletim)));
    }

    @Test
    void obtainsSellRateForUsdBrl() {
        when(client.queryCotacao(anyMap()))
            .thenReturn(response("5.2345", "2026-08-14 13:10:02.123", "Fechamento"));

        Optional<TaxaCambio> result =
            provider.obtain(new ParMoedas(new CodigoMoeda("USD"), new CodigoMoeda("BRL")));

        assertThat(result).isPresent();
        assertThat(result.get().taxa()).isEqualByComparingTo("5.2345");
        assertThat(result.get().vigencia()).isEqualTo(Instant.parse("2026-08-14T16:10:02.123Z"));
        verify(client).queryCotacao(argThat(params ->
            "'USD'".equals(params.get("@moeda"))
                && "json".equals(params.get("$format"))
                && "cotacaoVenda,dataHoraCotacao,tipoBoletim".equals(params.get("$select"))
                && params.get("@dataInicial") != null
                && params.get("@dataFinalCotacao") != null));
    }

    @Test
    void parsesMicrosecondTimestamp() {
        when(client.queryCotacao(anyMap()))
            .thenReturn(response("5.2345", "2026-08-14 13:10:02.123456", "Fechamento"));

        Optional<TaxaCambio> result =
            provider.obtain(new ParMoedas(new CodigoMoeda("USD"), new CodigoMoeda("BRL")));

        assertThat(result).isPresent();
        assertThat(result.get().vigencia()).isEqualTo(Instant.parse("2026-08-14T16:10:02.123456Z"));
    }

    @Test
    void picksMostRecentFechamento() {
        PtaxResponse response = new PtaxResponse(List.of(
            new PtaxQuote("5.10", "2026-08-13 13:09:15.558931", "Fechamento"),
            new PtaxQuote("5.22", "2026-08-14 13:10:22.94166", "Fechamento")));
        when(client.queryCotacao(anyMap())).thenReturn(response);

        Optional<TaxaCambio> result =
            provider.obtain(new ParMoedas(new CodigoMoeda("USD"), new CodigoMoeda("BRL")));

        assertThat(result).isPresent();
        assertThat(result.get().taxa()).isEqualByComparingTo("5.22");
    }

    @Test
    void mapsFeignFailureToProviderUnavailable() {
        Request request = Request.create(
            Request.HttpMethod.GET, "http://localhost/olinda", Map.of(), null, StandardCharsets.UTF_8);
        when(client.queryCotacao(anyMap()))
            .thenThrow(new feign.RetryableException(503, "boom", Request.HttpMethod.GET, 0L, request));

        assertThatThrownBy(() -> provider.obtain(new ParMoedas(new CodigoMoeda("USD"), new CodigoMoeda("BRL"))))
            .isInstanceOf(ExchangeRateProviderUnavailableException.class);
    }

    @Test
    void usesInverseRateForBrlUsd() {
        when(client.queryCotacao(anyMap()))
            .thenReturn(response("5.2345", "2026-08-14 13:10:02.123", "Fechamento"));

        Optional<TaxaCambio> result =
            provider.obtain(new ParMoedas(new CodigoMoeda("BRL"), new CodigoMoeda("USD")));

        assertThat(result).isPresent();
        assertThat(result.get().taxa())
            .isEqualByComparingTo(BigDecimal.ONE.divide(new BigDecimal("5.2345"), 8, RoundingMode.HALF_EVEN));
    }

    @Test
    void returnsEmptyForPairWithoutBrl() {
        Optional<TaxaCambio> result =
            provider.obtain(new ParMoedas(new CodigoMoeda("EUR"), new CodigoMoeda("USD")));

        assertThat(result).isEmpty();
        verifyNoInteractions(client);
    }

    @Test
    void returnsEmptyForPairWithoutConfiguredQuote() {
        Optional<TaxaCambio> result =
            provider.obtain(new ParMoedas(new CodigoMoeda("BRL"), new CodigoMoeda("JPY")));

        assertThat(result).isEmpty();
        verifyNoInteractions(client);
    }

    @Test
    void returnsEmptyWhenBodyIsEmpty() {
        when(client.queryCotacao(anyMap())).thenReturn(new PtaxResponse(List.of()));

        Optional<TaxaCambio> result =
            provider.obtain(new ParMoedas(new CodigoMoeda("USD"), new CodigoMoeda("BRL")));

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyWhenBodyIsNull() {
        when(client.queryCotacao(anyMap())).thenReturn(null);

        Optional<TaxaCambio> result =
            provider.obtain(new ParMoedas(new CodigoMoeda("USD"), new CodigoMoeda("BRL")));

        assertThat(result).isEmpty();
    }

    @Test
    void picksFirstQuoteWhenNoFechamento() {
        when(client.queryCotacao(anyMap()))
            .thenReturn(response("5.0000", "2026-08-14 09:00:00.000", "Abertura"));

        Optional<TaxaCambio> result =
            provider.obtain(new ParMoedas(new CodigoMoeda("USD"), new CodigoMoeda("BRL")));

        assertThat(result).isPresent();
        assertThat(result.get().taxa()).isEqualByComparingTo("5.0000");
    }
}