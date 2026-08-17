package com.srm.creditengine.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
class OpenApiContractTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void apiDocsExposeOperationSummariesAndResponses() {
        ResponseEntity<String> response = restTemplate.getForEntity("/v3/api-docs", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        String body = response.getBody();
        assertThat(body)
            .contains("\"Atualiza a taxa de câmbio vigente de um par de moedas\"")
            .contains("\"Consulta a taxa de câmbio vigente\"")
            .contains("\"Busca e persiste a taxa de câmbio via integração externa\"")
            .contains("\"Converte um valor entre moedas\"")
            .contains("\"Simula a precificação de um ativo\"")
            .contains("\"Cria um recebível\"")
            .contains("\"Lista recebíveis\"")
            .contains("\"Liquida um lote de recebíveis\"")
            .contains("\"Consulta uma liquidação\"")
            .contains("\"Consulta o extrato de liquidações\"")
            .contains("\"Provedor externo indisponível\"")
            .contains("\"Já existe taxa vigente para o par informado\"")
            .contains("\"/api/taxas-cambio\"")
            .contains("\"/api/simulacoes/precificacao\"")
            .contains("\"/api/recebiveis\"")
            .contains("\"/api/liquidacoes\"");
    }
}