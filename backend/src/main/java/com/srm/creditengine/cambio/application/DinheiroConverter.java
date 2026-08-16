package com.srm.creditengine.cambio.application;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.shared.domain.exception.IncompatibleCurrenciesException;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import com.srm.creditengine.shared.domain.model.Dinheiro;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class DinheiroConverter {

    public Dinheiro convert(Dinheiro valor, TaxaCambio taxaCambio) {
        ParMoedas par = taxaCambio.par();
        validateParContem(valor.moeda(), par);
        BigDecimal factor = factorFor(valor.moeda(), par, taxaCambio.taxa());
        CodigoMoeda target = otherMoeda(valor.moeda(), par);
        return new Dinheiro(valor.valor().multiply(factor), target, valor.escala());
    }

    private void validateParContem(CodigoMoeda moeda, ParMoedas par) {
        if (!par.contem(moeda)) {
            throw new IncompatibleCurrenciesException(moeda, par.cotacao());
        }
    }

    private BigDecimal factorFor(CodigoMoeda moeda, ParMoedas par, BigDecimal taxa) {
        if (moeda.equals(par.base())) {
            return taxa;
        }
        return BigDecimal.ONE.divide(taxa, 12, RoundingMode.HALF_EVEN);
    }

    private CodigoMoeda otherMoeda(CodigoMoeda moeda, ParMoedas par) {
        return moeda.equals(par.base()) ? par.cotacao() : par.base();
    }
}