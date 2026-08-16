package com.srm.creditengine.cambio.infrastructure.adapter.out.external;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.srm.creditengine.cambio.infrastructure.adapter.out.external.BcbPtaxTaxaCambioProvider.PtaxResponse;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class BcbPtaxClientContractTest {

    private static final String ODATA_PATH =
        "/CotacaoMoedaPeriodo(moeda=@moeda,dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)";

    static final WireMockServer wireMockServer = new WireMockServer(options().dynamicPort());

    static {
        wireMockServer.start();
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("app.cambio.provider.base-url", wireMockServer::baseUrl);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @Autowired
    private BcbPtaxClient client;

    @Test
    void queriesOdataEndpointAndDeserializesJson() {
        wireMockServer.stubFor(get(urlPathEqualTo(ODATA_PATH))
            .withQueryParam("@moeda", equalTo("'USD'"))
            .withQueryParam("@dataInicial", equalTo("'08-10-2026'"))
            .withQueryParam("$format", equalTo("json"))
            .withQueryParam("$select", equalTo("cotacaoVenda,dataHoraCotacao,tipoBoletim"))
            .willReturn(okJson("{\"value\":[{\"cotacaoVenda\":\"5.2345\",\"dataHoraCotacao\":\"2026-08-14 13:10:02.123\",\"tipoBoletim\":\"Fechamento\"}]}")));

        PtaxResponse response = client.queryCotacao(Map.of(
            "@moeda", "'USD'",
            "@dataInicial", "'08-10-2026'",
            "@dataFinalCotacao", "'08-16-2026'",
            "$format", "json",
            "$select", "cotacaoVenda,dataHoraCotacao,tipoBoletim"));

        assertThat(response.value()).hasSize(1);
        assertThat(response.value().get(0).cotacaoVenda()).isEqualTo("5.2345");
        assertThat(response.value().get(0).dataHoraCotacao()).isEqualTo("2026-08-14 13:10:02.123");
        assertThat(response.value().get(0).tipoBoletim()).isEqualTo("Fechamento");
    }
}