package com.srm.creditengine.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

import com.srm.creditengine.auth.domain.AccessToken;
import com.srm.creditengine.auth.domain.PasswordHasher;
import com.srm.creditengine.auth.domain.RefreshToken;
import com.srm.creditengine.auth.domain.RefreshTokenRepository;
import com.srm.creditengine.auth.domain.Role;
import com.srm.creditengine.auth.domain.TokenPair;
import com.srm.creditengine.auth.domain.TokenProvider;
import com.srm.creditengine.auth.domain.Usuario;
import com.srm.creditengine.auth.domain.UsuarioRepository;
import com.srm.creditengine.auth.domain.exception.AccountLockedException;
import com.srm.creditengine.auth.domain.exception.InvalidCredentialsException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenProvider tokenProvider;

    @BeforeEach
    void resetMocks() {
        org.mockito.Mockito.reset(usuarioRepository, refreshTokenRepository, passwordHasher, tokenProvider);
    }

    @Test
    void loginIssuesAccessAndRefreshTokens() {
        Instant now = Instant.parse("2026-08-16T12:00:00Z");
        Usuario usuario = new Usuario(1L, "admin", "hash", Role.ADMIN, false, 0, null);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordHasher.matches("admin123", "hash")).thenReturn(true);
        when(tokenProvider.issueAccessToken(usuario))
            .thenReturn(new AccessToken("jwt", now.plusSeconds(900)));
        when(tokenProvider.generateRefreshToken()).thenReturn("raw-refresh");
        when(tokenProvider.hashRefreshToken("raw-refresh")).thenReturn("hashed");
        when(tokenProvider.refreshTokenExpiry()).thenReturn(now.plusSeconds(604800));

        Login login = new Login(usuarioRepository, refreshTokenRepository, passwordHasher, tokenProvider);
        TokenPair pair = login.login("admin", "admin123", now);

        assertThat(pair.accessToken().value()).isEqualTo("jwt");
        assertThat(pair.accessToken().expiresAt()).isEqualTo(now.plusSeconds(900));
        assertThat(pair.refreshToken()).isEqualTo("raw-refresh");
        assertThat(pair.refreshTokenExpiresAt()).isEqualTo(now.plusSeconds(604800));

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().tokenHash()).isEqualTo("hashed");
        assertThat(captor.getValue().usuarioId()).isEqualTo(1L);
        assertThat(captor.getValue().revoked()).isFalse();
    }

    @Test
    void loginRejectsUnknownUser() {
        when(usuarioRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        Login login = new Login(usuarioRepository, refreshTokenRepository, passwordHasher, tokenProvider);

        assertThatThrownBy(() -> login.login("nobody", "pass", Instant.now()))
            .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginRejectsWrongPassword() {
        Usuario usuario = new Usuario(1L, "admin", "hash", Role.ADMIN, false, 0, null);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordHasher.matches("wrong", "hash")).thenReturn(false);

        Login login = new Login(usuarioRepository, refreshTokenRepository, passwordHasher, tokenProvider);

        assertThatThrownBy(() -> login.login("admin", "wrong", Instant.now()))
            .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void loginLocksAccountAfterFiveFailedAttempts() {
        Instant now = Instant.parse("2026-08-16T12:00:00Z");
        
        Usuario usuario = new Usuario(1L, "admin", "hash", Role.ADMIN, false, 4, null);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordHasher.matches("wrong", "hash")).thenReturn(false);
        when(usuarioRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        Login login = new Login(usuarioRepository, refreshTokenRepository, passwordHasher, tokenProvider);
        
        assertThatThrownBy(() -> login.login("admin", "wrong", now))
            .isInstanceOf(InvalidCredentialsException.class);
        
        var savedUser = org.mockito.ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().failedLoginAttempts()).isEqualTo(5);
        assertThat(savedUser.getValue().lockedUntil()).isNotNull();
    }

    @Test
    void loginRejectsLockedAccount() {
        Instant now = Instant.parse("2026-08-16T12:00:00Z");
        Usuario usuario = new Usuario(1L, "admin", "hash", Role.ADMIN, false, 5, now.plusSeconds(900));
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));

        Login login = new Login(usuarioRepository, refreshTokenRepository, passwordHasher, tokenProvider);

        assertThatThrownBy(() -> login.login("admin", "admin123", now))
            .isInstanceOf(AccountLockedException.class);
    }

    @Test
    void loginResetsFailedAttemptsOnSuccess() {
        Instant now = Instant.parse("2026-08-16T12:00:00Z");
        Usuario usuario = new Usuario(1L, "admin", "hash", Role.ADMIN, false, 3, null);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));
        when(passwordHasher.matches("admin123", "hash")).thenReturn(true);
        when(tokenProvider.issueAccessToken(any(Usuario.class)))
            .thenReturn(new AccessToken("jwt", now.plusSeconds(900)));
        when(tokenProvider.generateRefreshToken()).thenReturn("raw-refresh");
        when(tokenProvider.hashRefreshToken("raw-refresh")).thenReturn("hashed");
        when(tokenProvider.refreshTokenExpiry()).thenReturn(now.plusSeconds(604800));

        Login login = new Login(usuarioRepository, refreshTokenRepository, passwordHasher, tokenProvider);
        login.login("admin", "admin123", now);

        verify(usuarioRepository).save(usuario.resetFailedAttempts());
    }
}