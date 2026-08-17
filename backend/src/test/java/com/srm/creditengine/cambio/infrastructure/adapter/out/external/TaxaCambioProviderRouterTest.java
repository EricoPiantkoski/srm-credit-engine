package com.srm.creditengine.cambio.infrastructure.adapter.out.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaxaCambioProviderRouterTest {

    private static final ParMoedas BRL_USD = new ParMoedas(new CodigoMoeda("BRL"), new CodigoMoeda("USD"));
    private static final ParMoedas USD_BRL = new ParMoedas(new CodigoMoeda("USD"), new CodigoMoeda("BRL"));

    @Mock
    private BcbPtaxTaxaCambioProvider bcb;

    @Mock
    private AwesomeApiBrcProvider awesome;

    private TaxaCambioProviderRouter router() {
        return new TaxaCambioProviderRouter(bcb, awesome);
    }

    @Test
    void routesBrlUsdToAwesome() {
        TaxaCambio taxa = new TaxaCambio(BRL_USD, new BigDecimal("0.19153790"), Instant.parse("2026-08-14T21:04:53Z"));
        when(awesome.supports(BRL_USD)).thenReturn(true);
        when(awesome.obtain(BRL_USD)).thenReturn(Optional.of(taxa));

        Optional<TaxaCambio> result = router().obtain(BRL_USD);

        assertThat(result).contains(taxa);
        verifyNoInteractions(bcb);
    }

    @Test
    void routesUsdBrlToBcb() {
        TaxaCambio taxa = new TaxaCambio(USD_BRL, new BigDecimal("5.25"), Instant.parse("2026-08-14T16:00:00Z"));
        when(awesome.supports(USD_BRL)).thenReturn(false);
        when(bcb.obtain(USD_BRL)).thenReturn(Optional.of(taxa));

        Optional<TaxaCambio> result = router().obtain(USD_BRL);

        assertThat(result).contains(taxa);
        verify(bcb).obtain(USD_BRL);
    }

    @Test
    void propagatesEmpty() {
        when(awesome.supports(USD_BRL)).thenReturn(false);
        when(bcb.obtain(USD_BRL)).thenReturn(Optional.empty());

        Optional<TaxaCambio> result = router().obtain(USD_BRL);

        assertThat(result).isEmpty();
    }
}