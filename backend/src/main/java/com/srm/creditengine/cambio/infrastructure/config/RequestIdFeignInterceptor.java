package com.srm.creditengine.cambio.infrastructure.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class RequestIdFeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String requestId = MDC.get("requestId");
        if (requestId != null) {
            template.header("X-Request-Id", requestId);
        }
    }
}