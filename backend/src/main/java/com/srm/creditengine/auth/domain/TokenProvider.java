package com.srm.creditengine.auth.domain;

import java.time.Instant;

public interface TokenProvider {

    AccessToken issueAccessToken(Usuario usuario);

    String generateRefreshToken();

    String hashRefreshToken(String rawToken);

    Instant refreshTokenExpiry();
}
