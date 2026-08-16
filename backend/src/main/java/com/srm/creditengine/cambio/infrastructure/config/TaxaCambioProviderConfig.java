package com.srm.creditengine.cambio.infrastructure.config;

import com.srm.creditengine.cambio.application.DinheiroConverter;
import com.srm.creditengine.cambio.application.TaxaCambioOrchestrator;
import com.srm.creditengine.cambio.application.TaxaCambioUpdater;
import com.srm.creditengine.cambio.application.TaxaVigenteReader;
import com.srm.creditengine.cambio.domain.MoedaRepository;
import com.srm.creditengine.cambio.domain.TaxaCambioProvider;
import com.srm.creditengine.cambio.domain.TaxaCambioRepository;
import com.srm.creditengine.cambio.infrastructure.adapter.out.external.BcbPtaxClient;
import com.srm.creditengine.cambio.infrastructure.adapter.out.external.BcbPtaxTaxaCambioProvider;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TaxaCambioProviderConfig {

    @Bean
    public TaxaCambioProvider taxaCambioProvider(BcbPtaxClient bcbPtaxClient,
            @Value("${app.cambio.provider.quote-currency}") String quoteCurrency) {
        return new BcbPtaxTaxaCambioProvider(bcbPtaxClient, quoteCurrency, ZoneId.of("America/Sao_Paulo"), 3);
    }

    @Bean
    public TaxaCambioUpdater taxaCambioUpdater(TaxaCambioRepository repository, MoedaRepository moedaRepository) {
        return new TaxaCambioUpdater(repository, moedaRepository);
    }

    @Bean
    public TaxaVigenteReader taxaVigenteReader(TaxaCambioRepository repository, TaxaCambioProvider provider) {
        return new TaxaVigenteReader(repository, provider);
    }

    @Bean
    public DinheiroConverter dinheiroConverter() {
        return new DinheiroConverter();
    }

    @Bean
    public TaxaCambioOrchestrator taxaCambioOrchestrator(TaxaCambioProvider provider,
            TaxaCambioRepository repository, MoedaRepository moedaRepository) {
        return new TaxaCambioOrchestrator(provider, repository, moedaRepository);
    }
}