package com.srm.creditengine.infrastructure.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@Configuration
@OpenAPIDefinition(info = @Info(title = "SRM Credit Engine API", version = "1.0.0"))
public class OpenApiConfig {
}
