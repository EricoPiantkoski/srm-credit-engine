package com.srm.creditengine.audit.infrastructure.adapter.out.persistence;

import com.srm.creditengine.audit.domain.AuditLog;
import com.srm.creditengine.audit.domain.AuditLogRepository;
import org.springframework.stereotype.Component;

@Component
public class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final AuditLogJpaRepository jpaRepository;

    public AuditLogRepositoryAdapter(AuditLogJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void registrar(AuditLog log) {
        jpaRepository.save(new AuditLogJpaEntity(
            log.username(), log.acao(), log.recurso(), log.resultado().name(),
            log.chaveIdempotencia(), log.requestId(), log.createdAt()));
    }
}