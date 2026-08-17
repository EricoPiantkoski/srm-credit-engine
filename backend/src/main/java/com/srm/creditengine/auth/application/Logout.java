package com.srm.creditengine.auth.application;

import com.srm.creditengine.auth.domain.RefreshToken;
import com.srm.creditengine.auth.domain.RefreshTokenRepository;
import com.srm.creditengine.auth.domain.TokenProvider;
import com.srm.creditengine.auth.domain.exception.InvalidRefreshTokenException;
import java.time.Instant;

public class Logout {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenProvider tokenProvider;

    public Logout(RefreshTokenRepository refreshTokenRepository, TokenProvider tokenProvider) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenProvider = tokenProvider;
    }

    public void logout(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(tokenProvider.hashRefreshToken(rawRefreshToken))
            .ifPresentOrElse(
                token -> {
                    if (!token.revoked()) {
                        refreshTokenRepository.save(token.revogar());
                    }
                },
                () -> { throw new InvalidRefreshTokenException(); });
    }
}
