package com.srm.creditengine.cambio.infrastructure.config;

import feign.Retryer;

public class BackoffRetryer extends Retryer.Default {

    public BackoffRetryer() {
        super(100L, 1000L, 3);
    }
}