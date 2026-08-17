package com.srm.creditengine.auth.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.srm.creditengine.auth.domain.RefreshToken;
import com.srm.creditengine.auth.domain.RefreshTokenRepository;
import com.srm.creditengine.auth.domain.TokenProvider;
import com.srm.creditengine.auth.domain.exception.InvalidRefreshTokenException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogoutTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenProvider tokenProvider;

    @Test
    void logoutRevokesActiveToken() {
        RefreshToken token = new RefreshToken(5L, "hashed", 1L, Instant.parse("2026-08-23T12:00:00Z"), false);
        when(tokenProvider.hashRefreshToken("raw")).thenReturn("hashed");
        when(refreshTokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(token.revogar())).thenReturn(token.revogar());

        Logout logout = new Logout(refreshTokenRepository, tokenProvider);
        logout.logout("raw");

        verify(refreshTokenRepository).save(token.revogar());
    }

    @Test
    void logoutIgnoresAlreadyRevokedToken() {
        RefreshToken token = new RefreshToken(5L, "hashed", 1L, Instant.parse("2026-08-23T12:00:00Z"), true);
        when(tokenProvider.hashRefreshToken("raw")).thenReturn("hashed");
        when(refreshTokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(token));

        Logout logout = new Logout(refreshTokenRepository, tokenProvider);
        logout.logout("raw");
    }

    @Test
    void logoutRejectsUnknownToken() {
        when(tokenProvider.hashRefreshToken("raw")).thenReturn("hashed");
        when(refreshTokenRepository.findByTokenHash("hashed")).thenReturn(Optional.empty());

        Logout logout = new Logout(refreshTokenRepository, tokenProvider);

        assertThatThrownBy(() -> logout.logout("raw"))
            .isInstanceOf(InvalidRefreshTokenException.class);
    }
}