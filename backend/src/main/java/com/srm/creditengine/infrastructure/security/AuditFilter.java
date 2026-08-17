package com.srm.creditengine.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.srm.creditengine.audit.domain.AuditLog;
import com.srm.creditengine.audit.domain.AuditLogRepository;
import com.srm.creditengine.audit.domain.ResultadoAuditoria;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

public class AuditFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuditFilter.class);

    private static final List<String> SENSITIVE_PREFIXES = List.of(
        "/api/recebiveis",
        "/api/taxas-cambio",
        "/api/liquidacoes",
        "/api/auth/login",
        "/api/auth/refresh",
        "/api/auth/logout");

    private static final Map<String, String> ACAO_POR_PREFIXO = Map.of(
        "/api/recebiveis", "CRIAR_RECEBIVEL",
        "/api/taxas-cambio", "ATUALIZAR_TAXA",
        "/api/liquidacoes", "LIQUIDAR_LOTE",
        "/api/auth/login", "LOGIN",
        "/api/auth/refresh", "REFRESH",
        "/api/auth/logout", "LOGOUT");

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditFilter(AuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!isElegivel(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request);
        try {
            filterChain.doFilter(wrapped, response);
        } finally {
            registrar(wrapped, response);
        }
    }

    private boolean isElegivel(HttpServletRequest request) {
        if (!isWriteMethod(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        return SENSITIVE_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private boolean isWriteMethod(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method)
            || "PATCH".equals(method);
    }

    private void registrar(HttpServletRequest request, HttpServletResponse response) {
        try {
            ResultadoAuditoria resultado = response.getStatus() >= 400
                ? ResultadoAuditoria.FALHA : ResultadoAuditoria.SUCESSO;
            repository.registrar(AuditLog.novo(
                currentUser(request),
                acaoPara(request.getRequestURI()),
                request.getRequestURI(),
                resultado,
                chaveIdempotencia(request),
                (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE)));
        } catch (Exception e) {
            log.warn("falha ao registrar auditoria para {}", request.getRequestURI(), e);
        }
    }

    private String currentUser(HttpServletRequest request) {
        String username = usernameDoBody(request);
        if (username != null) {
            return username;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
            && !"anonymousUser".equals(authentication.getName())) {
            return authentication.getName();
        }
        return null;
    }

    private String usernameDoBody(HttpServletRequest request) {
        return campoDoBody(request, "username");
    }

    private String chaveIdempotencia(HttpServletRequest request) {
        return campoDoBody(request, "chaveIdempotencia");
    }

    private String campoDoBody(HttpServletRequest request, String campo) {
        if (!(request instanceof ContentCachingRequestWrapper wrapped)) {
            return null;
        }
        byte[] body = wrapped.getContentAsByteArray();
        if (body.length == 0) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(new String(body, StandardCharsets.UTF_8));
            JsonNode value = node.get(campo);
            return value == null || value.isNull() ? null : value.asText();
        } catch (IOException e) {
            return null;
        }
    }

    private String acaoPara(String path) {
        return ACAO_POR_PREFIXO.entrySet().stream()
            .filter(entry -> path.startsWith(entry.getKey()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse("ESCRITA");
    }
}