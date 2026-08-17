package com.srm.creditengine.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.srm.creditengine.auth.domain.AccessToken;
import com.srm.creditengine.auth.domain.RefreshToken;
import com.srm.creditengine.auth.domain.RefreshTokenRepository;
import com.srm.creditengine.auth.domain.Role;
import com.srm.creditengine.auth.domain.TokenPair;
import com.srm.creditengine.auth.domain.TokenProvider;
import com.srm.creditengine.auth.domain.Usuario;
import com.srm.creditengine.auth.domain.UsuarioRepository;
import com.srm.creditengine.auth.domain.exception.InvalidRefreshTokenException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenProvider tokenProvider;

    @Test
    void refreshRotatesAndReissuesTokens() {
        Instant now = Instant.parse("2026-08-16T12:00:00Z");
        RefreshToken existing = new RefreshToken(5L, "hashed-old", 1L, now.plusSeconds(3600), false);
        Usuario usuario = new Usuario(1L, "admin", "hash", Role.ADMIN, false);
        when(tokenProvider.hashRefreshToken("raw-old")).thenReturn("hashed-old");
        when(refreshTokenRepository.findByTokenHash("hashed-old")).thenReturn(Optional.of(existing));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(refreshTokenRepository.save(existing.revogar())).thenReturn(existing.revogar());
        when(tokenProvider.issueAccessToken(usuario))
            .thenReturn(new AccessToken("jwt-new", now.plusSeconds(900)));
        when(tokenProvider.generateRefreshToken()).thenReturn("raw-new");
        when(tokenProvider.hashRefreshToken("raw-new")).thenReturn("hashed-new");
        when(tokenProvider.refreshTokenExpiry()).thenReturn(now.plusSeconds(604800));

        Refresh refresh = new Refresh(usuarioRepository, refreshTokenRepository, tokenProvider);
        TokenPair pair = refresh.refresh("raw-old", now);

        assertThat(pair.accessToken().value()).isEqualTo("jwt-new");
        assertThat(pair.refreshToken()).isEqualTo("raw-new");

        ArgumentCaptor<RefreshToken> newToken = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(newToken.capture());
        assertThat(newToken.getAllValues().get(0).revoked()).isTrue();
        assertThat(newToken.getAllValues().get(1).tokenHash()).isEqualTo("hashed-new");
        assertThat(newToken.getAllValues().get(1).revoked()).isFalse();
    }

    @Test
    void refreshRejectsUnknownToken() {
        Instant now = Instant.parse("2026-08-16T12:00:00Z");
        when(tokenProvider.hashRefreshToken("raw-unknown")).thenReturn("hashed-unknown");
        when(refreshTokenRepository.findByTokenHash("hashed-unknown")).thenReturn(Optional.empty());

        Refresh refresh = new Refresh(usuarioRepository, refreshTokenRepository, tokenProvider);

        assertThatThrownBy(() -> refresh.refresh("raw-unknown", now))
            .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refreshRejectsRevokedToken() {
        Instant now = Instant.parse("2026-08-16T12:00:00Z");
        RefreshToken revoked = new RefreshToken(5L, "hashed", 1L, now.plusSeconds(3600), true);
        when(tokenProvider.hashRefreshToken("raw")).thenReturn("hashed");
        when(refreshTokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(revoked));

        Refresh refresh = new Refresh(usuarioRepository, refreshTokenRepository, tokenProvider);

        assertThatThrownBy(() -> refresh.refresh("raw", now))
            .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refreshRejectsExpiredToken() {
        Instant now = Instant.parse("2026-08-16T12:00:00Z");
        RefreshToken expired = new RefreshToken(5L, "hashed", 1L, now.minusSeconds(1), false);
        when(tokenProvider.hashRefreshToken("raw")).thenReturn("hashed");
        when(refreshTokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(expired));

        Refresh refresh = new Refresh(usuarioRepository, refreshTokenRepository, tokenProvider);

        assertThatThrownBy(() -> refresh.refresh("raw", now))
            .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refreshRejectsWhenUserDeleted() {
        Instant now = Instant.parse("2026-08-16T12:00:00Z");
        RefreshToken existing = new RefreshToken(5L, "hashed", 99L, now.plusSeconds(3600), false);
        when(tokenProvider.hashRefreshToken("raw")).thenReturn("hashed");
        when(refreshTokenRepository.findByTokenHash("hashed")).thenReturn(Optional.of(existing));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        Refresh refresh = new Refresh(usuarioRepository, refreshTokenRepository, tokenProvider);

        assertThatThrownBy(() -> refresh.refresh("raw", now))
            .isInstanceOf(InvalidRefreshTokenException.class);
    }
}