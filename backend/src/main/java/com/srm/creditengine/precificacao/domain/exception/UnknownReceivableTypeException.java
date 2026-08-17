package com.srm.creditengine.precificacao.domain.exception;

import com.srm.creditengine.shared.domain.exception.DomainException;

public class UnknownReceivableTypeException extends DomainException {
    public UnknownReceivableTypeException(String codigo) {
        super("no pricing strategy configured for tipo recebivel: " + codigo);
    }
}