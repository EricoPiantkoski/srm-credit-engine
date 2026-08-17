package com.srm.creditengine.auth.domain.exception;

import com.srm.creditengine.shared.domain.exception.DomainException;

public class InvalidRefreshTokenException extends DomainException {

    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token.");
    }
}
