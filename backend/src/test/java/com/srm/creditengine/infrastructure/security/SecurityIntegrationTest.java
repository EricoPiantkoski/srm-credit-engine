package com.srm.creditengine.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.srm.creditengine.audit.infrastructure.adapter.out.persistence.AuditLogJpaRepository;
import com.srm.creditengine.auth.application.Login;
import com.srm.creditengine.auth.domain.TokenPair;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
class SecurityIntegrationTest {

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

    @Autowired
    Login login;

    @Autowired
    AuditLogJpaRepository auditLogJpaRepository;

    @Test
    void healthIsPublic() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/health", Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "UP");
    }

    @Test
    void protectedEndpointRejectsAnonymous() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/recebiveis", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedEndpointAcceptsAdminToken() {
        TokenPair tokens = login.login("admin", "admin123", Instant.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.accessToken().value());
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/recebiveis", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void prometheusEndpointRejectsAnonymous() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void prometheusEndpointAcceptsAdminTokenAndReturnsMetrics() {
        TokenPair tokens = login.login("admin", "admin123", Instant.now());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokens.accessToken().value());
        ResponseEntity<String> response = restTemplate.exchange(
            "/actuator/prometheus", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("jvm_memory_used_bytes");
    }

    @Test
    void loginRejectsWrongPassword() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange("/api/auth/login", HttpMethod.POST,
            new HttpEntity<>("{\"username\":\"admin\",\"password\":\"wrong\"}", headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void loginIsRecordedInAuditLog() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.exchange("/api/auth/login", HttpMethod.POST,
            new HttpEntity<>("{\"username\":\"admin\",\"password\":\"admin123\"}", headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(auditLogJpaRepository.findAll())
            .anyMatch(log -> "LOGIN".equals(log.getAcao())
                && "SUCESSO".equals(log.getResultado())
                && "admin".equals(log.getUsername()));
    }
}
