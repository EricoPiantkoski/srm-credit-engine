package com.srm.creditengine.precificacao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.srm.creditengine.precificacao.application.RecebivelCreator.CreateRecebivelInput;
import com.srm.creditengine.precificacao.domain.MoedaCatalog;
import com.srm.creditengine.precificacao.domain.Recebivel;
import com.srm.creditengine.precificacao.domain.RecebivelRepository;
import com.srm.creditengine.precificacao.domain.TipoRecebivel;
import com.srm.creditengine.precificacao.domain.TipoRecebivelRepository;
import com.srm.creditengine.precificacao.domain.exception.ReceivableConflictException;
import com.srm.creditengine.precificacao.domain.exception.UnknownCurrencyException;
import com.srm.creditengine.precificacao.domain.exception.UnknownReceivableTypeException;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecebivelCreatorTest {

    private static final CreateRecebivelInput INPUT = new CreateRecebivelInput(
        "REF-001", "DUPLICATA_MERCANTIL", new BigDecimal("1000.00"),
        "BRL", LocalDate.of(2026, 9, 15), "Cedente");

    @Mock
    private RecebivelRepository repository;

    @Mock
    private TipoRecebivelRepository tipoRepository;

    @Mock
    private MoedaCatalog moedaCatalog;

    private RecebivelCreator creator() {
        return new RecebivelCreator(repository, tipoRepository, moedaCatalog);
    }

    @Test
    void createsAndPersists() {
        when(tipoRepository.obtainByCodigo("DUPLICATA_MERCANTIL"))
            .thenReturn(Optional.of(new TipoRecebivel("DUPLICATA_MERCANTIL", "Duplicata Mercantil", new BigDecimal("0.015"))));
        when(moedaCatalog.scaleOf(new CodigoMoeda("BRL"))).thenReturn(2);
        when(repository.existsReferenciaExterna("REF-001")).thenReturn(false);

        Recebivel recebivel = creator().create(INPUT);

        assertThat(recebivel.id()).isNull();
        assertThat(recebivel.referenciaExterna()).isEqualTo("REF-001");
        assertThat(recebivel.codigoTipo()).isEqualTo("DUPLICATA_MERCANTIL");
        assertThat(recebivel.valorFace().valor()).isEqualByComparingTo("1000.00");
        assertThat(recebivel.valorFace().moeda().codigo()).isEqualTo("BRL");
        assertThat(recebivel.valorFace().escala()).isEqualTo(2);
        assertThat(recebivel.dataVencimento()).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(recebivel.cedente()).isEqualTo("Cedente");
        assertThat(recebivel.version()).isZero();
        verify(repository).save(any(Recebivel.class));
    }

    @Test
    void throwsUnknownTipo() {
        when(tipoRepository.obtainByCodigo("DUPLICATA_MERCANTIL")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> creator().create(INPUT))
            .isInstanceOf(UnknownReceivableTypeException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void throwsUnknownCurrency() {
        when(tipoRepository.obtainByCodigo("DUPLICATA_MERCANTIL"))
            .thenReturn(Optional.of(new TipoRecebivel("DUPLICATA_MERCANTIL", "Duplicata Mercantil", new BigDecimal("0.015"))));
        when(moedaCatalog.scaleOf(new CodigoMoeda("BRL"))).thenThrow(new UnknownCurrencyException("BRL"));

        assertThatThrownBy(() -> creator().create(INPUT))
            .isInstanceOf(UnknownCurrencyException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void throwsConflictOnDuplicateReferencia() {
        when(tipoRepository.obtainByCodigo("DUPLICATA_MERCANTIL"))
            .thenReturn(Optional.of(new TipoRecebivel("DUPLICATA_MERCANTIL", "Duplicata Mercantil", new BigDecimal("0.015"))));
        when(moedaCatalog.scaleOf(new CodigoMoeda("BRL"))).thenReturn(2);
        when(repository.existsReferenciaExterna("REF-001")).thenReturn(true);

        assertThatThrownBy(() -> creator().create(INPUT))
            .isInstanceOf(ReceivableConflictException.class)
            .hasMessageContaining("REF-001");
        verify(repository, never()).save(any());
    }
}