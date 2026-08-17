package com.srm.creditengine.precificacao.infrastructure.config;

import com.srm.creditengine.precificacao.application.PrecificacaoEngine;
import com.srm.creditengine.precificacao.application.PrecificacaoSimulator;
import com.srm.creditengine.precificacao.application.RecebivelCreator;
import com.srm.creditengine.precificacao.application.RecebivelQuery;
import com.srm.creditengine.precificacao.domain.CambioGateway;
import com.srm.creditengine.precificacao.domain.ChequePreDatadoStrategy;
import com.srm.creditengine.precificacao.domain.DuplicataMercantilStrategy;
import com.srm.creditengine.precificacao.domain.MoedaCatalog;
import com.srm.creditengine.precificacao.domain.PrecificacaoStrategyResolver;
import com.srm.creditengine.precificacao.domain.RecebivelRepository;
import com.srm.creditengine.precificacao.domain.TipoRecebivelRepository;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PrecificacaoConfig {

    @Bean
    public DuplicataMercantilStrategy duplicataMercantilStrategy(TipoRecebivelRepository repository) {
        return new DuplicataMercantilStrategy(repository);
    }

    @Bean
    public ChequePreDatadoStrategy chequePreDatadoStrategy(TipoRecebivelRepository repository) {
        return new ChequePreDatadoStrategy(repository);
    }

    @Bean
    public PrecificacaoStrategyResolver precificacaoStrategyResolver(
            DuplicataMercantilStrategy duplicata, ChequePreDatadoStrategy cheque) {
        return new PrecificacaoStrategyResolver(Map.of("DUPLICATA_MERCANTIL", duplicata, "CHEQUE_PRE_DATADO", cheque));
    }

    @Bean
    public PrecificacaoEngine precificacaoEngine(CambioGateway cambioGateway,
            @Value("${app.precificacao.taxa-base}") BigDecimal taxaBase) {
        return new PrecificacaoEngine(cambioGateway, taxaBase);
    }

    @Bean
    public RecebivelCreator recebivelCreator(RecebivelRepository repository,
            TipoRecebivelRepository tipoRepository, MoedaCatalog moedaCatalog) {
        return new RecebivelCreator(repository, tipoRepository, moedaCatalog);
    }

    @Bean
    public RecebivelQuery recebivelQuery(RecebivelRepository repository) {
        return new RecebivelQuery(repository);
    }

    @Bean
    public PrecificacaoSimulator precificacaoSimulator(
            PrecificacaoStrategyResolver resolver, PrecificacaoEngine engine, MoedaCatalog moedaCatalog) {
        return new PrecificacaoSimulator(resolver, engine, moedaCatalog);
    }
}