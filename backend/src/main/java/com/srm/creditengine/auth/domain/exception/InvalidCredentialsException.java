package com.srm.creditengine.auth.domain.exception;

import com.srm.creditengine.shared.domain.exception.DomainException;

public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("Invalid username or password.");
    }
}
