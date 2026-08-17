package com.srm.creditengine.precificacao.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.srm.creditengine.cambio.infrastructure.adapter.out.persistence.MoedaJpaEntity;
import com.srm.creditengine.cambio.infrastructure.adapter.out.persistence.MoedaJpaRepository;
import com.srm.creditengine.precificacao.domain.exception.UnknownCurrencyException;
import com.srm.creditengine.shared.domain.model.CodigoMoeda;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MoedaCatalogAdapterTest {

    @Mock
    private MoedaJpaRepository jpaRepository;

    @Test
    void returnsExistingScale() {
        MoedaJpaEntity entity = mock(MoedaJpaEntity.class);
        when(entity.getEscala()).thenReturn(2);
        when(jpaRepository.findById("BRL")).thenReturn(Optional.of(entity));

        assertThat(new MoedaCatalogAdapter(jpaRepository).scaleOf(new CodigoMoeda("BRL"))).isEqualTo(2);
    }

    @Test
    void throwsForMissingCurrency() {
        when(jpaRepository.findById("EUR")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new MoedaCatalogAdapter(jpaRepository).scaleOf(new CodigoMoeda("EUR")))
            .isInstanceOf(UnknownCurrencyException.class);
    }
}