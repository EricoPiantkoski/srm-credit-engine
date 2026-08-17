package com.srm.creditengine.precificacao.domain.exception;

import com.srm.creditengine.shared.domain.exception.DomainException;

public class ReceivableConflictException extends DomainException {
    public ReceivableConflictException(String referenciaExterna) {
        super("recebivel already exists with referenciaExterna: " + referenciaExterna);
    }
}