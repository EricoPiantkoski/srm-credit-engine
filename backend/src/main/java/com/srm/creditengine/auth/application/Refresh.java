package com.srm.creditengine.auth.application;

import com.srm.creditengine.auth.domain.AccessToken;
import com.srm.creditengine.auth.domain.RefreshToken;
import com.srm.creditengine.auth.domain.RefreshTokenRepository;
import com.srm.creditengine.auth.domain.TokenPair;
import com.srm.creditengine.auth.domain.TokenProvider;
import com.srm.creditengine.auth.domain.Usuario;
import com.srm.creditengine.auth.domain.UsuarioRepository;
import com.srm.creditengine.auth.domain.exception.InvalidRefreshTokenException;
import java.time.Instant;

public class Refresh {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenProvider tokenProvider;

    public Refresh(UsuarioRepository usuarioRepository, RefreshTokenRepository refreshTokenRepository,
                   TokenProvider tokenProvider) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenProvider = tokenProvider;
    }

    public TokenPair refresh(String rawRefreshToken, Instant now) {
        RefreshToken existing = refreshTokenRepository
            .findByTokenHash(tokenProvider.hashRefreshToken(rawRefreshToken))
            .orElseThrow(InvalidRefreshTokenException::new);

        if (existing.revoked() || existing.isExpired(now)) {
            throw new InvalidRefreshTokenException();
        }

        Usuario usuario = usuarioRepository.findById(existing.usuarioId())
            .orElseThrow(InvalidRefreshTokenException::new);

        refreshTokenRepository.save(existing.revogar());

        AccessToken accessToken = tokenProvider.issueAccessToken(usuario);
        String rawNewRefresh = tokenProvider.generateRefreshToken();
        Instant refreshExpiry = tokenProvider.refreshTokenExpiry();
        refreshTokenRepository.save(new RefreshToken(
            null, tokenProvider.hashRefreshToken(rawNewRefresh), usuario.id(), refreshExpiry, false));

        return new TokenPair(accessToken, rawNewRefresh, refreshExpiry);
    }
}
