package com.srm.creditengine.auth.infrastructure.adapter.in.web;

import com.srm.creditengine.auth.application.Login;
import com.srm.creditengine.auth.application.Logout;
import com.srm.creditengine.auth.application.Refresh;
import com.srm.creditengine.auth.domain.TokenPair;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Autenticação", description = "Login, refresh e logout com JWT")
@RequestMapping("/api/auth")
public class AuthController {

    private final Login login;
    private final Refresh refresh;
    private final Logout logout;

    public AuthController(Login login, Refresh refresh, Logout logout) {
        this.login = login;
        this.refresh = refresh;
        this.logout = logout;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica e emite tokens",
        description = "Valida credenciais e retorna access token (curto) e refresh token (rotativo).")
    @ApiResponse(responseCode = "200", description = "Tokens emitidos")
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenPair tokens = login.login(request.username(), request.password(), Instant.now());
        return ResponseEntity.ok(toResponse(tokens));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova o par de tokens",
        description = "Rotaciona o refresh token e emite novo access token.")
    @ApiResponse(responseCode = "200", description = "Novo par de tokens")
    @ApiResponse(responseCode = "401", description = "Refresh token inválido, expirado ou revogado")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenPair tokens = refresh.refresh(request.refreshToken(), Instant.now());
        return ResponseEntity.ok(toResponse(tokens));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoga o refresh token",
        description = "Invalida o refresh token informado.")
    @ApiResponse(responseCode = "204", description = "Refresh token revogado")
    @ApiResponse(responseCode = "401", description = "Refresh token inválido")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        logout.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    private AuthResponse toResponse(TokenPair tokens) {
        return new AuthResponse(
            tokens.accessToken().value(),
            tokens.accessToken().expiresAt(),
            tokens.refreshToken(),
            tokens.refreshTokenExpiresAt());
    }

    public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password) {}

    public record RefreshRequest(
        @NotBlank String refreshToken) {}

    public record AuthResponse(
        String accessToken, Instant accessTokenExpiresAt,
        String refreshToken, Instant refreshTokenExpiresAt) {}
}
