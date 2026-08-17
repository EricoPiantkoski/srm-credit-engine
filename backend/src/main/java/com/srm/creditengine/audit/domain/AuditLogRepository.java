package com.srm.creditengine.audit.domain;

public interface AuditLogRepository {

    void registrar(AuditLog log);
}