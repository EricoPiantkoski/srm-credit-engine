package com.srm.creditengine.cambio.infrastructure.adapter.out.external;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.exception.ExchangeRateProviderUnavailableException;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test")
class CircuitBreakerIntegrationTest {

    private static final String PATH = "/json/last/BRL-USD";
    private static final ParMoedas BRL_USD = new ParMoedas(new CodigoMoeda("BRL"), new CodigoMoeda("USD"));

    static final WireMockServer wireMockServer = new WireMockServer(options().dynamicPort());

    static {
        wireMockServer.start();
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("app.cambio.awesome-api.base-url", wireMockServer::baseUrl);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("resilience4j.circuitbreaker.instances.awesomeApiBrc.sliding-window-size", () -> "4");
        registry.add("resilience4j.circuitbreaker.instances.awesomeApiBrc.minimum-number-of-calls", () -> "2");
        registry.add("resilience4j.circuitbreaker.instances.awesomeApiBrc.failure-rate-threshold", () -> "50");
        registry.add("resilience4j.circuitbreaker.instances.awesomeApiBrc.wait-duration-in-open-state", () -> "30s");
        registry.add("resilience4j.circuitbreaker.instances.awesomeApiBrc.permitted-number-of-calls-in-half-open-state", () -> "1");
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @Autowired
    private AwesomeApiBrcProvider provider;

    @Test
    void opensCircuitAndFallbackRespondsWithoutCallingProvider() {
        wireMockServer.stubFor(get(urlEqualTo(PATH))
            .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> provider.obtain(BRL_USD))
            .isInstanceOf(ExchangeRateProviderUnavailableException.class);
        assertThatThrownBy(() -> provider.obtain(BRL_USD))
            .isInstanceOf(ExchangeRateProviderUnavailableException.class);

        int callsAfterFailures = wireMockServer.findAll(getRequestedFor(urlEqualTo(PATH))).size();

        assertThatThrownBy(() -> provider.obtain(BRL_USD))
            .isInstanceOf(ExchangeRateProviderUnavailableException.class);

        assertThat(wireMockServer.findAll(getRequestedFor(urlEqualTo(PATH))).size())
            .isEqualTo(callsAfterFailures);
    }
}