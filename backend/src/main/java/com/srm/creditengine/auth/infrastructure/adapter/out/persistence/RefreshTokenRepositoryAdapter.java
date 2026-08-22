package com.srm.creditengine.auth.infrastructure.adapter.out.persistence;

import com.srm.creditengine.auth.domain.RefreshToken;
import com.srm.creditengine.auth.domain.RefreshTokenRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenJpaEntity entity;
        if (refreshToken.id() != null) {
            entity = jpaRepository.findById(refreshToken.id()).orElseThrow(() ->
                new IllegalStateException("refresh token not found for id: " + refreshToken.id()));
            entity.setRevoked(refreshToken.revoked());
            entity.setExpiresAt(refreshToken.expiresAt());
        } else {
            entity = new RefreshTokenJpaEntity(
                refreshToken.tokenHash(), refreshToken.usuarioId(), refreshToken.expiresAt(),
                refreshToken.revoked(), Instant.now());
        }
        RefreshTokenJpaEntity saved = jpaRepository.save(entity);
        return new RefreshToken(saved.getId(), saved.getTokenHash(), saved.getUsuarioId(),
            saved.getExpiresAt(), saved.isRevoked());
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(entity -> new RefreshToken(
            entity.getId(), entity.getTokenHash(), entity.getUsuarioId(), entity.getExpiresAt(),
            entity.isRevoked()));
    }

    @Override
    public Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash) {
        return jpaRepository.findByTokenHashForUpdate(tokenHash).map(entity -> new RefreshToken(
            entity.getId(), entity.getTokenHash(), entity.getUsuarioId(), entity.getExpiresAt(),
            entity.isRevoked()));
    }
}
