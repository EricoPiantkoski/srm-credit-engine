package com.srm.creditengine.auth.infrastructure.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.srm.creditengine.auth.domain.AccessToken;
import com.srm.creditengine.auth.domain.TokenProvider;
import com.srm.creditengine.auth.domain.Usuario;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import jakarta.annotation.PostConstruct;

public class JwtTokenProvider implements TokenProvider {

    private final JwtEncoder jwtEncoder;
    private final Duration accessTokenTtl;
    private final Duration refreshTokenTtl;
    private final String issuer;

    public JwtTokenProvider(String secret, Duration accessTokenTtl, Duration refreshTokenTtl, String issuer) {
        validateSecret(secret);
        this.jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKeyFor(secret)));
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
        this.issuer = issuer;
    }

    @PostConstruct
    public void validateSecret() {
    }

    private static void validateSecret(String secret) {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT_SECRET must be valid base64", e);
        }
        if (decoded.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 bytes (base64-decoded), but was: " + decoded.length + " bytes");
        }
    }

    public static SecretKey secretKeyFor(String secret) {
        return new SecretKeySpec(normalizeSecret(secret), "HmacSHA256");
    }

    @Override
    public AccessToken issueAccessToken(Usuario usuario) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTokenTtl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(issuer)
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(usuario.username())
            .claim("uid", usuario.id())
            .claim("roles", List.of(usuario.role().name()))
            .build();
        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).getTokenValue();
        return new AccessToken(value, expiresAt);
    }

    @Override
    public String generateRefreshToken() {
        return UUID.randomUUID().toString() + UUID.randomUUID();
    }

    @Override
    public String hashRefreshToken(String rawToken) {
        return sha256Hex(rawToken.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Instant refreshTokenExpiry() {
        return Instant.now().plus(refreshTokenTtl);
    }

    private static byte[] normalizeSecret(String secret) {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("JWT_SECRET must be valid base64", e);
        }
        return decoded;
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String sha256Hex(byte[] input) {
        return HexFormat.of().formatHex(sha256(input));
    }
}
