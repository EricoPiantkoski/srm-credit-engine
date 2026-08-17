package com.srm.creditengine.cambio.infrastructure.adapter.out.external;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.cambio.domain.TaxaCambioProvider;
import java.util.Optional;

public class TaxaCambioProviderRouter implements TaxaCambioProvider {

    private final BcbPtaxTaxaCambioProvider bcb;
    private final AwesomeApiBrcProvider awesome;

    public TaxaCambioProviderRouter(BcbPtaxTaxaCambioProvider bcb, AwesomeApiBrcProvider awesome) {
        this.bcb = bcb;
        this.awesome = awesome;
    }

    @Override
    public Optional<TaxaCambio> obtain(ParMoedas par) {
        if (awesome.supports(par)) {
            return awesome.obtain(par);
        }
        return bcb.obtain(par);
    }
}