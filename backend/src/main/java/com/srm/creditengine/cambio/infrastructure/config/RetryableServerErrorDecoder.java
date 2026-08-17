package com.srm.creditengine.cambio.infrastructure.config;

import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;

public class RetryableServerErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder delegate = new ErrorDecoder.Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() >= 500 && response.status() <= 599) {
            return new RetryableException(
                response.status(),
                "server error " + response.status(),
                response.request().httpMethod(),
                delegate.decode(methodKey, response),
                (Long) null,
                response.request());
        }
        return delegate.decode(methodKey, response);
    }
}