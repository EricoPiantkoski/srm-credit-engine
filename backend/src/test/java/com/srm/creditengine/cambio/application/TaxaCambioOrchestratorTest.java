package com.srm.creditengine.cambio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.srm.creditengine.cambio.domain.MoedaRepository;
import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.cambio.domain.TaxaCambioProvider;
import com.srm.creditengine.cambio.domain.TaxaCambioRepository;
import com.srm.creditengine.cambio.domain.exception.ExchangeRateProviderUnavailableException;
import com.srm.creditengine.cambio.domain.exception.UnknownCurrencyException;
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
class TaxaCambioOrchestratorTest {

    private static final ParMoedas PAR = new ParMoedas(new CodigoMoeda("USD"), new CodigoMoeda("BRL"));
    private static final TaxaCambio TAXA =
        new TaxaCambio(PAR, new BigDecimal("5.25"), Instant.parse("2026-08-14T16:00:00Z"));

    @Mock
    private TaxaCambioProvider provider;

    @Mock
    private TaxaCambioRepository repository;

    @Mock
    private MoedaRepository moedaRepository;

    @InjectMocks
    private TaxaCambioOrchestrator orchestrator;

    @Test
    void orchestrateSavesWhenVigenciaIsNew() {
        when(moedaRepository.exists(any())).thenReturn(true);
        when(provider.obtain(any())).thenReturn(Optional.of(TAXA));
        when(repository.existsVigencia(any(), any())).thenReturn(false);

        TaxaCambio result = orchestrator.orchestrate(PAR);

        assertThat(result).isEqualTo(TAXA);
        verify(repository).save(TAXA);
    }

    @Test
    void orchestrateDoesNotDuplicateExistingVigencia() {
        when(moedaRepository.exists(any())).thenReturn(true);
        when(provider.obtain(any())).thenReturn(Optional.of(TAXA));
        when(repository.existsVigencia(any(), any())).thenReturn(true);

        orchestrator.orchestrate(PAR);

        verify(repository, never()).save(any());
    }

    @Test
    void orchestrateThrowsWhenProviderUnavailable() {
        when(moedaRepository.exists(any())).thenReturn(true);
        when(provider.obtain(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orchestrator.orchestrate(PAR))
            .isInstanceOf(ExchangeRateProviderUnavailableException.class);
    }

    @Test
    void orchestrateThrowsWhenMoedaUnknown() {
        when(moedaRepository.exists(any())).thenReturn(false);

        assertThatThrownBy(() -> orchestrator.orchestrate(PAR))
            .isInstanceOf(UnknownCurrencyException.class);
    }
}