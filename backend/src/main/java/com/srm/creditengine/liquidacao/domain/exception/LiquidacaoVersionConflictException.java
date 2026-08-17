package com.srm.creditengine.liquidacao.domain.exception;

import com.srm.creditengine.shared.domain.exception.DomainException;

public class LiquidacaoVersionConflictException extends DomainException {
    public LiquidacaoVersionConflictException(Long recebivelId, Long expectedVersion) {
        super("recebivel " + recebivelId + " was modified by another transaction or is already liquidated "
            + "(expected version " + expectedVersion + "). Reprocess the liquidation with current data.");
}
}