package com.srm.creditengine.liquidacao.infrastructure.config;

import com.srm.creditengine.liquidacao.application.ConsultarLiquidacao;
import com.srm.creditengine.liquidacao.application.LiquidarLote;
import com.srm.creditengine.liquidacao.domain.RepositorioLiquidacao;
import com.srm.creditengine.precificacao.application.PrecificacaoEngine;
import com.srm.creditengine.precificacao.domain.PrecificacaoStrategyResolver;
import com.srm.creditengine.precificacao.domain.RecebivelRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiquidacaoConfig {

    @Bean
    public LiquidarLote liquidarLote(RepositorioLiquidacao repositorioLiquidacao,
                                     RecebivelRepository recebivelRepository,
                                     PrecificacaoStrategyResolver strategyResolver,
                                     PrecificacaoEngine engine) {
        return new LiquidarLote(repositorioLiquidacao, recebivelRepository, strategyResolver, engine);
    }

    @Bean
    public ConsultarLiquidacao consultarLiquidacao(RepositorioLiquidacao repositorioLiquidacao) {
        return new ConsultarLiquidacao(repositorioLiquidacao);
    }

}
