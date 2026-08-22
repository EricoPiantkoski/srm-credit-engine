package com.srm.creditengine.auth.application;

import com.srm.creditengine.auth.domain.AccessToken;
import com.srm.creditengine.auth.domain.PasswordHasher;
import com.srm.creditengine.auth.domain.RefreshToken;
import com.srm.creditengine.auth.domain.RefreshTokenRepository;
import com.srm.creditengine.auth.domain.TokenPair;
import com.srm.creditengine.auth.domain.TokenProvider;
import com.srm.creditengine.auth.domain.Usuario;
import com.srm.creditengine.auth.domain.UsuarioRepository;
import com.srm.creditengine.auth.domain.exception.AccountLockedException;
import com.srm.creditengine.auth.domain.exception.InvalidCredentialsException;
import java.time.Instant;

public class Login {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final TokenProvider tokenProvider;

    public Login(UsuarioRepository usuarioRepository, RefreshTokenRepository refreshTokenRepository,
                 PasswordHasher passwordHasher, TokenProvider tokenProvider) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.tokenProvider = tokenProvider;
    }

    public TokenPair login(String username, String rawPassword, Instant now) {
        Usuario usuario = usuarioRepository.findByUsername(username)
            .orElseThrow(InvalidCredentialsException::new);

        if (usuario.isLocked(now)) {
            throw new AccountLockedException(usuario.lockedUntil());
        }

        if (!passwordHasher.matches(rawPassword, usuario.passwordHash())) {
            Usuario updated = usuario.incrementFailedAttempts(now);
            usuarioRepository.save(updated);
            throw new InvalidCredentialsException();
        }

        Usuario updated = usuario.resetFailedAttempts();
        usuarioRepository.save(updated);

        AccessToken accessToken = tokenProvider.issueAccessToken(updated);
        String rawRefresh = tokenProvider.generateRefreshToken();
        Instant refreshExpiry = tokenProvider.refreshTokenExpiry();
        refreshTokenRepository.save(new RefreshToken(
            null, tokenProvider.hashRefreshToken(rawRefresh), updated.id(), refreshExpiry, false));

        return new TokenPair(accessToken, rawRefresh, refreshExpiry);
    }
}
