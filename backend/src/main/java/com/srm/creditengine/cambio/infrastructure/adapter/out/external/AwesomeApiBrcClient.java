package com.srm.creditengine.cambio.infrastructure.adapter.out.external;

import com.srm.creditengine.cambio.infrastructure.config.RequestIdFeignInterceptor;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "awesomeApiBrc", url = "${app.cambio.awesome-api.base-url}", configuration = RequestIdFeignInterceptor.class)
public interface AwesomeApiBrcClient {

    @GetMapping("/json/last/BRL-USD")
    AwesomeApiBrcResponse lastBrcUsd();

    record AwesomeApiBrcResponse(@JsonProperty("BRLUSD") AwesomeApiBrcQuote quote) {}

    record AwesomeApiBrcQuote(
        @JsonProperty("ask") String ask,
        @JsonProperty("create_date") String createDate) {}
}