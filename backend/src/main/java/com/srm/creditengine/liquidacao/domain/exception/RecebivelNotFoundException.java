package com.srm.creditengine.liquidacao.domain.exception;

import com.srm.creditengine.shared.domain.exception.DomainException;

public class RecebivelNotFoundException extends DomainException {
    public RecebivelNotFoundException(Long id) {
        super("recebivel not found with id: " + id);
    }
}