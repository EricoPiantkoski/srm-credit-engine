package com.srm.creditengine.cambio.infrastructure.adapter.out.external;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import feign.RetryableException;
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
class FeignRetryIntegrationTest {

    private static final String PATH = "/json/last/BRL-USD";

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
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @Autowired
    private AwesomeApiBrcClient client;

    @Test
    void retriesWithBackoffAndPropagatesAfterExhaustingAttempts() {
        wireMockServer.stubFor(get(urlEqualTo(PATH))
            .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(client::lastBrcUsd)
            .isInstanceOf(RetryableException.class);

        wireMockServer.verify(3, getRequestedFor(urlEqualTo(PATH)));
    }
}