package com.srm.creditengine.cambio.infrastructure.adapter.out.external;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.srm.creditengine.cambio.infrastructure.adapter.out.external.AwesomeApiBrcClient.AwesomeApiBrcResponse;
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
class AwesomeApiBrcClientContractTest {

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
    void queriesLastEndpointAndDeserializesJson() {
        wireMockServer.stubFor(get(urlPathEqualTo(PATH))
            .willReturn(okJson("{\"BRLUSD\":{\"bid\":\"0.1914279\",\"ask\":\"0.1915379\"," +
                "\"create_date\":\"2026-08-14 18:04:53\"}}")));

        AwesomeApiBrcResponse response = client.lastBrcUsd();

        assertThat(response.quote()).isNotNull();
        assertThat(response.quote().ask()).isEqualTo("0.1915379");
        assertThat(response.quote().createDate()).isEqualTo("2026-08-14 18:04:53");
    }
}