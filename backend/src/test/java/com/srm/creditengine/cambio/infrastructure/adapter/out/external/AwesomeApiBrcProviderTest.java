package com.srm.creditengine.cambio.infrastructure.adapter.out.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.cambio.domain.exception.ExchangeRateProviderUnavailableException;
import com.srm.creditengine.cambio.infrastructure.adapter.out.external.AwesomeApiBrcClient.AwesomeApiBrcQuote;
import com.srm.creditengine.cambio.infrastructure.adapter.out.external.AwesomeApiBrcClient.AwesomeApiBrcResponse;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import feign.Request;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AwesomeApiBrcProviderTest {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    @Mock
    private AwesomeApiBrcClient client;

    private AwesomeApiBrcProvider provider;

    @BeforeEach
    void setUp() {
        provider = new AwesomeApiBrcProvider(client, SAO_PAULO);
    }

    @Test
    void obtainsAskRateForBrlUsd() {
        when(client.lastBrcUsd()).thenReturn(
            new AwesomeApiBrcResponse(new AwesomeApiBrcQuote("0.1915379", "2026-08-14 18:04:53")));

        Optional<TaxaCambio> result =
            provider.obtain(new ParMoedas(new CodigoMoeda("BRL"), new CodigoMoeda("USD")));

        assertThat(result).isPresent();
        assertThat(result.get().taxa()).isEqualByComparingTo(new BigDecimal("0.19153790"));
        assertThat(result.get().vigencia()).isEqualTo(java.time.Instant.parse("2026-08-14T21:04:53Z"));
    }

    @Test
    void returnsEmptyForUnsupportedPair() {
        Optional<TaxaCambio> result =
            provider.obtain(new ParMoedas(new CodigoMoeda("USD"), new CodigoMoeda("BRL")));

        assertThat(result).isEmpty();
        assertThat(provider.supports(new ParMoedas(new CodigoMoeda("BRL"), new CodigoMoeda("USD")))).isTrue();
        assertThat(provider.supports(new ParMoedas(new CodigoMoeda("USD"), new CodigoMoeda("BRL")))).isFalse();
    }

    @Test
    void returnsEmptyWhenBodyIsNull() {
        when(client.lastBrcUsd()).thenReturn(null);

        Optional<TaxaCambio> result =
            provider.obtain(new ParMoedas(new CodigoMoeda("BRL"), new CodigoMoeda("USD")));

        assertThat(result).isEmpty();
    }

    @Test
    void returnsEmptyWhenAskMissing() {
        when(client.lastBrcUsd()).thenReturn(new AwesomeApiBrcResponse(null));

        Optional<TaxaCambio> result =
            provider.obtain(new ParMoedas(new CodigoMoeda("BRL"), new CodigoMoeda("USD")));

        assertThat(result).isEmpty();
    }

    @Test
    void mapsFeignFailureToProviderUnavailable() {
        Request request = Request.create(
            Request.HttpMethod.GET, "http://localhost/json/last/BRL-USD", Map.of(), null, StandardCharsets.UTF_8);
        when(client.lastBrcUsd()).thenThrow(new feign.RetryableException(503, "boom", Request.HttpMethod.GET, 0L, request));

        assertThatThrownBy(() -> provider.obtain(new ParMoedas(new CodigoMoeda("BRL"), new CodigoMoeda("USD"))))
            .isInstanceOf(ExchangeRateProviderUnavailableException.class);
    }

    @Test
    void fallbackRethrowsProviderUnavailable() throws Exception {
        ParMoedas par = new ParMoedas(new CodigoMoeda("BRL"), new CodigoMoeda("USD"));
        java.lang.reflect.Method fallback =
            AwesomeApiBrcProvider.class.getDeclaredMethod("obtainFallback", ParMoedas.class, Throwable.class);
        fallback.setAccessible(true);

        assertThatThrownBy(() ->
            fallback.invoke(provider, par, new RuntimeException("boom")))
            .hasRootCauseInstanceOf(ExchangeRateProviderUnavailableException.class);
    }
}