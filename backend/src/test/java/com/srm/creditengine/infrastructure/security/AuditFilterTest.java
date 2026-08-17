package com.srm.creditengine.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srm.creditengine.audit.domain.AuditLog;
import com.srm.creditengine.audit.domain.AuditLogRepository;
import com.srm.creditengine.audit.domain.ResultadoAuditoria;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AuditFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AuditLogRepository repository = mock(AuditLogRepository.class);
    private final AuditFilter filter = new AuditFilter(repository, objectMapper);

    private FilterChain chainThatReadsBody() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        doAnswer(invocation -> {
            invocation.getArgument(0, HttpServletRequest.class).getInputStream().readAllBytes();
            return null;
        }).when(chain).doFilter(any(), any());
        return chain;
    }

    @Test
    void recordsSuccessForSensitiveWriteWithIdempotencyKey() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("admin", null, java.util.List.of()));
        try {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/liquidacoes");
            request.setContent(
                "{\"chaveIdempotencia\":\"11111111-1111-1111-1111-111111111111\"}".getBytes(StandardCharsets.UTF_8));
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, chainThatReadsBody());

            ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
            verify(repository).registrar(captor.capture());
            AuditLog log = captor.getValue();
            assertThat(log.acao()).isEqualTo("LIQUIDAR_LOTE");
            assertThat(log.resultado()).isEqualTo(ResultadoAuditoria.SUCESSO);
            assertThat(log.username()).isEqualTo("admin");
            assertThat(log.chaveIdempotencia()).isEqualTo("11111111-1111-1111-1111-111111111111");
            assertThat(log.createdAt()).isNotNull();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void recordsFailureWhenStatusIs4xx() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/taxas-cambio/USD-BRL");
        request.setContent("{}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);
        doAnswer(invocation -> {
            invocation.getArgument(0, HttpServletRequest.class).getInputStream().readAllBytes();
            invocation.getArgument(1, MockHttpServletResponse.class).setStatus(409);
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).registrar(captor.capture());
        assertThat(captor.getValue().acao()).isEqualTo("ATUALIZAR_TAXA");
        assertThat(captor.getValue().resultado()).isEqualTo(ResultadoAuditoria.FALHA);
    }

    @Test
    void extractsUsernameFromLoginBodyWhenAnonymous() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setContent(
            "{\"username\":\"admin\",\"password\":\"admin123\"}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chainThatReadsBody());

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).registrar(captor.capture());
        assertThat(captor.getValue().acao()).isEqualTo("LOGIN");
        assertThat(captor.getValue().username()).isEqualTo("admin");
    }

    @Test
    void doesNotRecordNonSensitiveRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        verify(repository, never()).registrar(any());
    }

    @Test
    void doesNotRecordReadRequestsOnSensitivePaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/recebiveis");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(FilterChain.class));

        verify(repository, never()).registrar(any());
    }

    @Test
    void swallowsAuditRepositoryFailures() throws Exception {
        org.mockito.Mockito.doThrow(new RuntimeException("db down"))
            .when(repository).registrar(any());
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/recebiveis");
        request.setContent("{}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, chainThatReadsBody());

        assertThat(response.getStatus()).isEqualTo(200);
    }
}