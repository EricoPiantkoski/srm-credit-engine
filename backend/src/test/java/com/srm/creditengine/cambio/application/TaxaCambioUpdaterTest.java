package com.srm.creditengine.cambio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.srm.creditengine.cambio.domain.MoedaRepository;
import com.srm.creditengine.cambio.domain.ParMoedas;
import com.srm.creditengine.cambio.domain.TaxaCambio;
import com.srm.creditengine.cambio.domain.TaxaCambioRepository;
import com.srm.creditengine.cambio.domain.exception.ExchangeRateConflictException;
import com.srm.creditengine.cambio.domain.exception.UnknownCurrencyException;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaxaCambioUpdaterTest {

    private static final ParMoedas PAR = new ParMoedas(new CodigoMoeda("USD"), new CodigoMoeda("BRL"));
    private static final Instant VIGENCIA = Instant.parse("2026-08-14T16:00:00Z");

    @Mock
    private TaxaCambioRepository repository;

    @Mock
    private MoedaRepository moedaRepository;

    @InjectMocks
    private TaxaCambioUpdater updater;

    @Test
    void updateSavesWhenVigenciaAvailable() {
        when(moedaRepository.exists(any())).thenReturn(true);
        when(repository.existsVigencia(any(), any())).thenReturn(false);

        TaxaCambio result = updater.update(PAR, new BigDecimal("5.25"), VIGENCIA);

        assertThat(result.par()).isEqualTo(PAR);
        assertThat(result.taxa()).isEqualByComparingTo("5.25");
        assertThat(result.vigencia()).isEqualTo(VIGENCIA);
        verify(repository).save(result);
    }

    @Test
    void updateRejectsUnknownBase() {
        when(moedaRepository.exists(any())).thenReturn(false);

        assertThatThrownBy(() -> updater.update(PAR, new BigDecimal("5.25"), VIGENCIA))
            .isInstanceOf(UnknownCurrencyException.class)
            .hasMessageContaining("USD");
    }

    @Test
    void updateRejectsUnknownCotacao() {
        when(moedaRepository.exists(any()))
            .thenAnswer(inv -> !inv.getArgument(0, CodigoMoeda.class).codigo().equals("BRL"));

        assertThatThrownBy(() -> updater.update(PAR, new BigDecimal("5.25"), VIGENCIA))
            .isInstanceOf(UnknownCurrencyException.class)
            .hasMessageContaining("BRL");
    }

    @Test
    void updateRejectsOccupiedVigencia() {
        when(moedaRepository.exists(any())).thenReturn(true);
        when(repository.existsVigencia(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> updater.update(PAR, new BigDecimal("5.25"), VIGENCIA))
            .isInstanceOf(ExchangeRateConflictException.class);
    }
}