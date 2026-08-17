package com.srm.creditengine.precificacao.infrastructure.adapter.out.cambio;

import com.srm.creditengine.cambio.application.DinheiroConverter;
import com.srm.creditengine.cambio.application.TaxaVigenteReader;
import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.precificacao.domain.CambioGateway;
import com.srm.creditengine.precificacao.domain.TaxaCambioAplicada;
import com.srm.creditengine.precificacao.domain.exception.ExchangeRateUnavailableException;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import com.srm.creditengine.shared.domain.model.Dinheiro;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class CambioGatewayAdapter implements CambioGateway {

    private final TaxaVigenteReader taxaVigenteReader;
    private final DinheiroConverter dinheiroConverter;

    public CambioGatewayAdapter(TaxaVigenteReader taxaVigenteReader, DinheiroConverter dinheiroConverter) {
        this.taxaVigenteReader = taxaVigenteReader;
        this.dinheiroConverter = dinheiroConverter;
    }

    @Override
    public TaxaCambioAplicada convert(Dinheiro valor, CodigoMoeda moedaPagamento, Instant precificacaoReference) {
        ParMoedas par = new ParMoedas(valor.moeda(), moedaPagamento);
        TaxaCambio taxa = taxaVigenteReader.readOrObtain(par, precificacaoReference)
            .orElseThrow(() -> new ExchangeRateUnavailableException(valor.moeda(), moedaPagamento));
        Dinheiro converted = dinheiroConverter.convert(valor, taxa);
        return new TaxaCambioAplicada(converted, taxa.taxa(), taxa.vigencia());
    }
}