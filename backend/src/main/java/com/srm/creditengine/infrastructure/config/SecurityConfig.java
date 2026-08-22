package com.srm.creditengine.infrastructure.config;

import com.srm.creditengine.auth.application.Login;
import com.srm.creditengine.auth.application.Logout;
import com.srm.creditengine.auth.application.Refresh;
import com.srm.creditengine.auth.domain.PasswordHasher;
import com.srm.creditengine.auth.domain.RefreshTokenRepository;
import com.srm.creditengine.auth.domain.TokenProvider;
import com.srm.creditengine.auth.domain.UsuarioRepository;
import com.srm.creditengine.auth.infrastructure.security.BcryptPasswordHasher;
import com.srm.creditengine.auth.infrastructure.security.JwtTokenProvider;
import com.srm.creditengine.audit.domain.AuditLogRepository;
import com.srm.creditengine.infrastructure.security.AuthRateLimitFilter;
import com.srm.creditengine.infrastructure.security.AuditFilter;
import com.srm.creditengine.infrastructure.security.RateLimitFilter;
import com.srm.creditengine.infrastructure.security.RequestIdFilter;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordHasher passwordHasher() {
        return new BcryptPasswordHasher();
    }

    @Bean
    public TokenProvider tokenProvider(@Value("${app.security.jwt.secret}") String secret,
                                       @Value("${app.security.jwt.access-token-ttl}") Duration accessTtl,
                                       @Value("${app.security.jwt.refresh-token-ttl}") Duration refreshTtl,
                                       @Value("${app.security.jwt.issuer:SRM-CREDIT-ENGINE}") String issuer) {
        return new JwtTokenProvider(secret, accessTtl, refreshTtl, issuer);
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.security.jwt.secret}") String secret) {
        return NimbusJwtDecoder.withSecretKey(JwtTokenProvider.secretKeyFor(secret)).build();
    }

    @Bean
    public Login login(UsuarioRepository usuarioRepository, RefreshTokenRepository refreshTokenRepository,
                       PasswordHasher passwordHasher, TokenProvider tokenProvider) {
        return new Login(usuarioRepository, refreshTokenRepository, passwordHasher, tokenProvider);
    }

    @Bean
    public Refresh refresh(UsuarioRepository usuarioRepository, RefreshTokenRepository refreshTokenRepository,
                           TokenProvider tokenProvider) {
        return new Refresh(usuarioRepository, refreshTokenRepository, tokenProvider);
    }

    @Bean
    public Logout logout(RefreshTokenRepository refreshTokenRepository, TokenProvider tokenProvider) {
        return new Logout(refreshTokenRepository, tokenProvider);
    }

@Bean
    public AuthRateLimitFilter authRateLimitFilter(
            @Value("${app.security.auth-rate-limit.capacity:5}") int capacity,
            @Value("${app.security.auth-rate-limit.refill-period:PT1M}") Duration refillPeriod) {
        return new AuthRateLimitFilter(capacity, refillPeriod);
    }

    @Bean
    public RateLimitFilter rateLimitFilter(
            @Value("${app.security.rate-limit.capacity:100}") int capacity,
            @Value("${app.security.rate-limit.refill-period:PT1M}") Duration refillPeriod) {
        return new RateLimitFilter(capacity, refillPeriod);
    }

    @Bean
    public RequestIdFilter requestIdFilter() {
        return new RequestIdFilter();
    }

    @Bean
    public AuditFilter auditFilter(AuditLogRepository auditLogRepository,
                                   com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new AuditFilter(auditLogRepository, objectMapper);
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<AuthRateLimitFilter> authRateLimitFilterRegistration(AuthRateLimitFilter filter) {
        FilterRegistrationBean<AuthRateLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RequestIdFilter> requestIdFilterRegistration(RequestIdFilter filter) {
        FilterRegistrationBean<RequestIdFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<AuditFilter> auditFilterRegistration(AuditFilter filter) {
        FilterRegistrationBean<AuditFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

@Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   RateLimitFilter rateLimitFilter,
                                                   AuthRateLimitFilter authRateLimitFilter,
                                                   RequestIdFilter requestIdFilter,
                                                   AuditFilter auditFilter,
                                                   @Value("${app.security.expose-docs:false}") boolean exposeDocs,
                                                   @Value("${app.security.content-security-policy:default-src 'self'}") String contentSecurityPolicy)
            throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(contentSecurityPolicy))
                .frameOptions(frame -> frame.deny()))
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                auth.requestMatchers("/api/health", "/api/health/readiness", "/api/auth/login", "/api/auth/refresh").permitAll();
                if (exposeDocs) {
                    auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                }
                auth.anyRequest().hasRole("ADMIN");
            })
            .addFilterBefore(authRateLimitFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(requestIdFilter, org.springframework.security.web.access.intercept.AuthorizationFilter.class)
            .addFilterAfter(auditFilter, org.springframework.security.web.access.intercept.AuthorizationFilter.class)
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        return jwt -> {
            List<String> roles = jwt.getClaimAsStringList("roles");
            List<SimpleGrantedAuthority> authorities = (roles == null ? List.<String>of() : roles).stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
            return new UsernamePasswordAuthenticationToken(jwt.getSubject(), jwt, authorities);
        };
    }
}
