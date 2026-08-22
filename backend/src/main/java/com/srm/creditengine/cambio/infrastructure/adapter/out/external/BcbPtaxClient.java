package com.srm.creditengine.cambio.infrastructure.adapter.out.external;

import com.srm.creditengine.cambio.infrastructure.config.RequestIdFeignInterceptor;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "bcbPtax", url = "${app.cambio.bcb-ptax.base-url}", configuration = RequestIdFeignInterceptor.class)
public interface BcbPtaxClient {

    @GetMapping("/CotacaoMoedaPeriodo(moeda=@moeda,dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)")
    BcbPtaxTaxaCambioProvider.PtaxResponse queryCotacao(@RequestParam Map<String, String> params);
}