package com.srm.creditengine.extrato.infrastructure.config;

import com.srm.creditengine.extrato.application.ExtratoLiquidacoes;
import com.srm.creditengine.extrato.domain.ConsultaLiquidacao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExtratoConfig {

    @Bean
    public ExtratoLiquidacoes extratoLiquidacoes(ConsultaLiquidacao consultaLiquidacao) {
        return new ExtratoLiquidacoes(consultaLiquidacao);
    }
}