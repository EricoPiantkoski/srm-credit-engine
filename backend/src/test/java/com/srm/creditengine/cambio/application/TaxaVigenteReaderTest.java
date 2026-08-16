package com.srm.creditengine.cambio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.cambio.domain.TaxaCambioProvider;
import com.srm.creditengine.cambio.domain.TaxaCambioRepository;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaxaVigenteReaderTest {

    private static final ParMoedas PAR = new ParMoedas(new CodigoMoeda("USD"), new CodigoMoeda("BRL"));

    @Mock
    private TaxaCambioRepository repository;

    @Mock
    private TaxaCambioProvider provider;

    @InjectMocks
    private TaxaVigenteReader reader;

    @Test
    void readReturnsTaxaWhenExists() {
        TaxaCambio taxa = new TaxaCambio(PAR, new BigDecimal("5.25"), Instant.parse("2026-08-14T16:00:00Z"));
        when(repository.obtainVigente(any(), any())).thenReturn(Optional.of(taxa));

        Optional<TaxaCambio> result = reader.read(PAR, Instant.parse("2026-08-15T16:00:00Z"));

        assertThat(result).contains(taxa);
    }

    @Test
    void readReturnsEmptyWhenMissing() {
        when(repository.obtainVigente(any(), any())).thenReturn(Optional.empty());

        Optional<TaxaCambio> result = reader.read(PAR, Instant.parse("2026-08-15T16:00:00Z"));

        assertThat(result).isEmpty();
    }

    @Test
    void readOrObtainUsesRepositoryWithoutCallingProvider() {
        TaxaCambio taxa = new TaxaCambio(PAR, new BigDecimal("5.25"), Instant.parse("2026-08-14T16:00:00Z"));
        when(repository.obtainVigente(any(), any())).thenReturn(Optional.of(taxa));

        Optional<TaxaCambio> result = reader.readOrObtain(PAR, Instant.parse("2026-08-15T16:00:00Z"));

        assertThat(result).contains(taxa);
        verifyNoInteractions(provider);
    }

    @Test
    void readOrObtainFallsBackToProviderWhenRepositoryEmpty() {
        TaxaCambio obtida = new TaxaCambio(PAR, new BigDecimal("5.2236"), Instant.parse("2026-08-14T16:10:22Z"));
        when(repository.obtainVigente(any(), any())).thenReturn(Optional.empty());
        when(provider.obtain(PAR)).thenReturn(Optional.of(obtida));

        Optional<TaxaCambio> result = reader.readOrObtain(PAR, Instant.parse("2026-08-15T16:00:00Z"));

        assertThat(result).contains(obtida);
    }

    @Test
    void readOrObtainReturnsEmptyWhenRepositoryAndProviderEmpty() {
        when(repository.obtainVigente(any(), any())).thenReturn(Optional.empty());
        when(provider.obtain(PAR)).thenReturn(Optional.empty());

        Optional<TaxaCambio> result = reader.readOrObtain(PAR, Instant.parse("2026-08-15T16:00:00Z"));

        assertThat(result).isEmpty();
    }
}